package com.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 有机物官能团竞赛核心服务。
 */
@Service
public class ChemRaceService {

    private static final int OPTIONS_PER_QUESTION = 4;
    private static final int DEFAULT_QUESTION_DURATION_SECONDS = 30;
    private static final boolean DEFAULT_AUTO_NEXT_ENABLED = true;
    private static final int DEFAULT_QUESTION_COUNT = 10;
    private static final int MIN_QUESTION_DURATION_SECONDS = 10;
    private static final int MAX_QUESTION_DURATION_SECONDS = 300;
    private static final int MAX_HISTORY_RECORDS = 500;
    private static final Logger log = LoggerFactory.getLogger(ChemRaceService.class);

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${chem-race.storage-path:./data/chem-race}")
    private String storagePath;

    private final List<TemplateQuestion> questionBank = buildQuestionBank();
    private final List<RaceHistoryRecord> raceHistory = new ArrayList<RaceHistoryRecord>();
    private final List<StudentAccount> globalStudentAccounts = new ArrayList<StudentAccount>();

    @PostConstruct
    public void initQuestionBankStorage() {
        loadPersistedCustomQuestions();
        loadRaceHistory();
        loadGlobalStudentAccounts();
    }

    public RoomSnapshot createRoom(String teacherName) {
        String normalizedTeacher = normalizeName(teacherName, "老师");
        String roomCode = generateRoomCode();
        Room room = new Room(roomCode, normalizedTeacher, buildQuestions(DEFAULT_QUESTION_COUNT),
                DEFAULT_QUESTION_DURATION_SECONDS, DEFAULT_AUTO_NEXT_ENABLED);
        room.questionCount = Math.max(1, Math.min(DEFAULT_QUESTION_COUNT, room.questions.size()));
        room.maxQuestionCount = questionBank.size();
        synchronized (globalStudentAccounts) {
            for (StudentAccount account : globalStudentAccounts) {
                room.studentAccounts.add(copyStudentAccount(account));
            }
        }
        room.participants.put(normalizedTeacher, new Participant(normalizedTeacher));
        rooms.put(roomCode, room);
        return toSnapshot(room);
    }

    public RoomSnapshot joinRoom(String roomCode, String studentName, String studentId) {
        Room room = getRoom(roomCode);
        String normalizedStudent = buildDisplayName(normalizeName(studentName, "同学"), trim(studentId));

        synchronized (room) {
            if (!room.studentAccounts.isEmpty()) {
                boolean matched = false;
                for (StudentAccount account : room.studentAccounts) {
                    if (account.displayName.equals(normalizedStudent)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    throw new IllegalArgumentException("该学生不在老师发布的参赛名单中");
                }
            }
            if (!room.participants.containsKey(normalizedStudent)) {
                room.participants.put(normalizedStudent, new Participant(normalizedStudent));
            }
            return toSnapshot(room);
        }
    }

    public RoomSnapshot startRace(String roomCode) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            room.status = "RUNNING";
            room.currentQuestionIndex = 0;
            room.questionStartedAt = System.currentTimeMillis();
            return toSnapshot(room);
        }
    }

    public RoomSnapshot nextQuestion(String roomCode) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            if ("FINISHED".equals(room.status)) {
                return toSnapshot(room);
            }

            int next = room.currentQuestionIndex + 1;
            if (next >= room.questions.size()) {
                room.status = "FINISHED";
                room.currentQuestionIndex = room.questions.size() - 1;
                saveRaceHistoryIfNeeded(room);
            } else {
                room.currentQuestionIndex = next;
                room.questionStartedAt = System.currentTimeMillis();
            }
            return toSnapshot(room);
        }
    }

    public AnswerResult submitAnswer(String roomCode, String studentName, String selectedGroup) {
        Room room = getRoom(roomCode);
        String normalizedStudent = normalizeName(studentName, "同学");

        synchronized (room) {
            if (!"RUNNING".equals(room.status)) {
                throw new IllegalStateException("比赛尚未开始或已结束");
            }

            Question currentQuestion = room.questions.get(room.currentQuestionIndex);
            Set<String> answered = room.answeredByQuestion
                    .computeIfAbsent(currentQuestion.id, k -> new HashSet<String>());

            if (answered.contains(normalizedStudent)) {
                throw new IllegalStateException("本题已作答，等待下一题");
            }

            Participant participant = room.participants.computeIfAbsent(normalizedStudent,
                    k -> new Participant(normalizedStudent));

            String normalizedAnswer = selectedGroup == null ? "" : selectedGroup.trim();
            boolean correct = currentQuestion.correctGroup.equals(normalizedAnswer);
            long elapsedSec = Math.max(0L, (System.currentTimeMillis() - room.questionStartedAt) / 1000L);
            int delta = correct ? (10 + Math.max(0, 5 - (int) (elapsedSec / 4L))) : -2;

            participant.totalAnswered += 1;
            if (correct) {
                participant.correctCount += 1;
            }
            participant.score = Math.max(0, participant.score + delta);
            participant.lastAnswerAt = System.currentTimeMillis();
            answered.add(normalizedStudent);

            List<RankItem> rank = buildLeaderboard(room);
            int position = 1;
            for (int i = 0; i < rank.size(); i++) {
                if (rank.get(i).studentName.equals(normalizedStudent)) {
                    position = i + 1;
                    break;
                }
            }

            AnswerResult result = new AnswerResult();
            result.correct = correct;
            result.correctGroup = currentQuestion.correctGroup;
            result.studentName = normalizedStudent;
            result.deltaScore = delta;
            result.totalScore = participant.score;
            result.rank = position;
            result.questionId = currentQuestion.id;
            return result;
        }
    }

    public RoomSnapshot getState(String roomCode) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            return toSnapshot(room);
        }
    }

    public List<RankItem> getLeaderboard(String roomCode) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            return buildLeaderboard(room);
        }
    }

    public List<QuestionTemplateView> getQuestionBank() {
        synchronized (questionBank) {
            List<QuestionTemplateView> views = new ArrayList<QuestionTemplateView>();
            for (TemplateQuestion t : questionBank) {
                views.add(toTemplateView(t));
            }
            return views;
        }
    }

    public int addCustomQuestions(List<CustomQuestionInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("题目列表不能为空");
        }

        int added = 0;
        synchronized (questionBank) {
            for (CustomQuestionInput input : inputs) {
                if (input == null) {
                    continue;
                }
                String moleculeName = trim(input.moleculeName);
                String formula = trim(input.formula);
                String structureHint = trim(input.structureHint);
                String correctGroup = trim(input.correctGroup);
                String chapter = trim(input.chapter);

                if (moleculeName.isEmpty() || formula.isEmpty() || structureHint.isEmpty() || correctGroup.isEmpty()) {
                    continue;
                }

                questionBank.add(new TemplateQuestion(
                        input.id,
                        moleculeName,
                        formula,
                        structureHint,
                        correctGroup,
                        chapter.isEmpty() ? "自定义" : chapter,
                        true));
                added += 1;
            }
        }

        if (added == 0) {
            throw new IllegalArgumentException("没有可用题目，请检查字段是否完整");
        }

        saveCustomQuestions();

        return added;
    }

    public QuestionTemplateView updateCustomQuestion(String questionId, CustomQuestionInput input) {
        String id = trim(questionId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("题目ID不能为空");
        }
        if (input == null) {
            throw new IllegalArgumentException("题目内容不能为空");
        }

        String moleculeName = trim(input.moleculeName);
        String formula = trim(input.formula);
        String structureHint = trim(input.structureHint);
        String correctGroup = trim(input.correctGroup);
        String chapter = trim(input.chapter);

        if (moleculeName.isEmpty() || formula.isEmpty() || structureHint.isEmpty() || correctGroup.isEmpty()) {
            throw new IllegalArgumentException("题目字段不完整");
        }

        synchronized (questionBank) {
            for (TemplateQuestion question : questionBank) {
                if (!id.equals(question.id)) {
                    continue;
                }
                if (!question.custom) {
                    throw new IllegalArgumentException("内置题目不支持修改");
                }
                question.moleculeName = moleculeName;
                question.formula = formula;
                question.structureHint = structureHint;
                question.correctGroup = correctGroup;
                question.chapter = chapter.isEmpty() ? "自定义" : chapter;
                saveCustomQuestions();
                return toTemplateView(question);
            }
        }

        throw new IllegalArgumentException("未找到可修改题目: " + id);
    }

    public boolean deleteCustomQuestion(String questionId) {
        String id = trim(questionId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("题目ID不能为空");
        }

        boolean removed = false;
        synchronized (questionBank) {
            for (int i = 0; i < questionBank.size(); i++) {
                TemplateQuestion question = questionBank.get(i);
                if (!id.equals(question.id)) {
                    continue;
                }
                if (!question.custom) {
                    throw new IllegalArgumentException("内置题目不支持删除");
                }
                questionBank.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            saveCustomQuestions();
        }
        return removed;
    }

    public RoomSnapshot finishRace(String roomCode) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            if (!"FINISHED".equals(room.status)) {
                room.status = "FINISHED";
                if (!room.questions.isEmpty()) {
                    room.currentQuestionIndex = Math.min(room.currentQuestionIndex, room.questions.size() - 1);
                } else {
                    room.currentQuestionIndex = 0;
                }
                saveRaceHistoryIfNeeded(room);
            }
            return toSnapshot(room);
        }
    }

    public RoomSnapshot addRoomStudentAccount(String roomCode, String studentName, String studentId) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            String normalizedName = trim(studentName);
            String normalizedId = trim(studentId);
            if (normalizedName.isEmpty()) {
                throw new IllegalArgumentException("学生姓名不能为空");
            }

            StudentAccount account = new StudentAccount();
            account.studentName = normalizedName;
            account.studentId = normalizedId;
            account.displayName = buildDisplayName(normalizedName, normalizedId);

            for (StudentAccount existing : room.studentAccounts) {
                if (existing.displayName.equals(account.displayName)) {
                    return toSnapshot(room);
                }
            }
            room.studentAccounts.add(account);
            return toSnapshot(room);
        }
    }

    public RoomSnapshot removeRoomStudentAccount(String roomCode, String studentName, String studentId) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            String normalizedName = trim(studentName);
            String normalizedId = trim(studentId);
            String displayName = buildDisplayName(normalizedName, normalizedId);
            for (int i = 0; i < room.studentAccounts.size(); i++) {
                StudentAccount account = room.studentAccounts.get(i);
                if (!displayName.equals(account.displayName)) {
                    continue;
                }
                room.studentAccounts.remove(i);
                room.participants.remove(account.displayName);
                break;
            }
            return toSnapshot(room);
        }
    }

    public List<StudentAccountView> getGlobalStudentAccounts() {
        synchronized (globalStudentAccounts) {
            return toStudentAccountViews(globalStudentAccounts);
        }
    }

    public List<StudentAccountView> addGlobalStudentAccount(String studentName, String studentId) {
        String normalizedName = trim(studentName);
        String normalizedId = trim(studentId);
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("学生姓名不能为空");
        }

        synchronized (globalStudentAccounts) {
            String displayName = buildDisplayName(normalizedName, normalizedId);
            for (StudentAccount account : globalStudentAccounts) {
                if (displayName.equals(account.displayName)) {
                    return toStudentAccountViews(globalStudentAccounts);
                }
            }
            StudentAccount account = new StudentAccount();
            account.studentName = normalizedName;
            account.studentId = normalizedId;
            account.displayName = displayName;
            globalStudentAccounts.add(account);
            saveGlobalStudentAccounts();
            return toStudentAccountViews(globalStudentAccounts);
        }
    }

    public List<StudentAccountView> removeGlobalStudentAccount(String studentName, String studentId) {
        String normalizedName = trim(studentName);
        String normalizedId = trim(studentId);
        String displayName = buildDisplayName(normalizedName, normalizedId);

        synchronized (globalStudentAccounts) {
            for (int i = 0; i < globalStudentAccounts.size(); i++) {
                StudentAccount account = globalStudentAccounts.get(i);
                if (!displayName.equals(account.displayName)) {
                    continue;
                }
                globalStudentAccounts.remove(i);
                break;
            }
            saveGlobalStudentAccounts();
            return toStudentAccountViews(globalStudentAccounts);
        }
    }

    public CsvExport exportLeaderboardCsv(String roomCode) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            StringBuilder csv = new StringBuilder();
            csv.append("名次,姓名,分数,正确题数,作答题数,正确率\n");
            List<RankItem> leaderboard = buildLeaderboard(room);
            for (RankItem item : leaderboard) {
                String accuracy = item.totalAnswered == 0
                        ? "0%"
                        : String.format("%.1f%%", (item.correctCount * 100.0) / item.totalAnswered);
                csv.append(item.rank).append(',')
                        .append(csvEscape(item.studentName)).append(',')
                        .append(item.score).append(',')
                        .append(item.correctCount).append(',')
                        .append(item.totalAnswered).append(',')
                        .append(csvEscape(accuracy))
                        .append('\n');
            }

            CsvExport export = new CsvExport();
            export.fileName = "chem-race-" + room.roomCode + "-leaderboard.csv";
            export.content = csv.toString();
            return export;
        }
    }

    public RoomSnapshot updateRoomSettings(String roomCode,
            Boolean autoNextEnabled,
            Integer questionDurationSeconds,
            Integer questionCount) {
        Room room = getRoom(roomCode);
        synchronized (room) {
            if (autoNextEnabled != null) {
                room.autoNextEnabled = autoNextEnabled.booleanValue();
            }

            if (questionDurationSeconds != null) {
                int duration = questionDurationSeconds.intValue();
                if (duration < MIN_QUESTION_DURATION_SECONDS || duration > MAX_QUESTION_DURATION_SECONDS) {
                    throw new IllegalArgumentException("每题时长需在 "
                            + MIN_QUESTION_DURATION_SECONDS + " 到 " + MAX_QUESTION_DURATION_SECONDS + " 秒之间");
                }
                room.questionDurationSeconds = duration;
                if ("RUNNING".equals(room.status)) {
                    room.questionStartedAt = System.currentTimeMillis();
                }
            }

            if (questionCount != null) {
                if (!"WAITING".equals(room.status)) {
                    throw new IllegalArgumentException("比赛开始后不能调整题目数量");
                }
                int maxCount = questionBank.size();
                int normalizedCount = questionCount.intValue();
                if (normalizedCount < 1 || normalizedCount > maxCount) {
                    throw new IllegalArgumentException("题目数量需在 1 到 " + maxCount + " 之间");
                }
                room.questions = buildQuestions(normalizedCount);
                room.questionCount = normalizedCount;
                room.maxQuestionCount = maxCount;
                room.currentQuestionIndex = 0;
                room.answeredByQuestion.clear();
            }

            return toSnapshot(room);
        }
    }

    public List<RaceHistoryView> getRaceHistory() {
        synchronized (raceHistory) {
            List<RaceHistoryView> views = new ArrayList<RaceHistoryView>();
            for (RaceHistoryRecord record : raceHistory) {
                views.add(toRaceHistoryView(record));
            }
            return views;
        }
    }

    public List<RaceHistoryView> getStudentHistory(String studentName) {
        String target = trim(studentName);
        if (target.isEmpty()) {
            throw new IllegalArgumentException("学生姓名不能为空");
        }

        synchronized (raceHistory) {
            List<RaceHistoryView> views = new ArrayList<RaceHistoryView>();
            for (RaceHistoryRecord record : raceHistory) {
                HistoryRankItem matched = null;
                for (HistoryRankItem item : record.leaderboard) {
                    if (target.equals(item.studentName)
                            || item.studentName.startsWith(target + "[")
                            || target.startsWith(item.studentName + "[")) {
                        matched = item;
                        break;
                    }
                }
                if (matched != null) {
                    RaceHistoryView view = new RaceHistoryView();
                    view.roomCode = record.roomCode;
                    view.teacherName = record.teacherName;
                    view.finishedAt = record.finishedAt;
                    view.totalQuestions = record.totalQuestions;
                    view.leaderboard = new ArrayList<HistoryRankItem>();
                    view.leaderboard.add(copyHistoryItem(matched));
                    views.add(view);
                }
            }
            return views;
        }
    }

    private Room getRoom(String roomCode) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            throw new IllegalArgumentException("roomCode 不能为空");
        }
        Room room = rooms.get(roomCode.trim().toUpperCase());
        if (room == null) {
            throw new IllegalArgumentException("房间不存在: " + roomCode);
        }
        return room;
    }

    private String normalizeName(String name, String fallbackPrefix) {
        String value = name == null ? "" : name.trim();
        if (!value.isEmpty()) {
            return value;
        }
        return fallbackPrefix + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    private String buildDisplayName(String name, String studentId) {
        String normalizedName = trim(name);
        String normalizedId = trim(studentId);
        if (normalizedId.isEmpty()) {
            return normalizedName;
        }
        return normalizedName + "[" + normalizedId + "]";
    }

    private String generateRoomCode() {
        String code;
        do {
            int value = ThreadLocalRandom.current().nextInt(100000, 999999);
            code = String.valueOf(value);
        } while (rooms.containsKey(code));
        return code;
    }

    private RoomSnapshot toSnapshot(Room room) {
        RoomSnapshot snapshot = new RoomSnapshot();
        snapshot.roomCode = room.roomCode;
        snapshot.teacherName = room.teacherName;
        snapshot.status = room.status;
        snapshot.currentQuestionIndex = room.currentQuestionIndex;
        snapshot.totalQuestions = room.questions.size();
        snapshot.participantCount = room.participants.size();
        snapshot.question = room.questions.get(room.currentQuestionIndex);
        snapshot.questionDurationSeconds = room.questionDurationSeconds;
        snapshot.questionCount = room.questionCount;
        snapshot.maxQuestionCount = room.maxQuestionCount;
        snapshot.remainingSeconds = calcRemainingSeconds(room);
        snapshot.autoNextEnabled = room.autoNextEnabled;
        snapshot.studentAccounts = new ArrayList<StudentAccountView>();
        for (StudentAccount account : room.studentAccounts) {
            StudentAccountView view = new StudentAccountView();
            view.studentName = account.studentName;
            view.studentId = account.studentId;
            view.displayName = account.displayName;
            snapshot.studentAccounts.add(view);
        }
        snapshot.leaderboard = buildLeaderboard(room);
        return snapshot;
    }

    private int calcRemainingSeconds(Room room) {
        if (!"RUNNING".equals(room.status)) {
            return room.questionDurationSeconds;
        }
        long elapsed = Math.max(0L, (System.currentTimeMillis() - room.questionStartedAt) / 1000L);
        long remaining = room.questionDurationSeconds - elapsed;
        return (int) Math.max(0L, remaining);
    }

    private List<RankItem> buildLeaderboard(Room room) {
        List<Participant> participants = new ArrayList<Participant>(room.participants.values());
        Collections.sort(participants, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                int scoreCompare = Integer.compare(o2.score, o1.score);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return Long.compare(o1.lastAnswerAt, o2.lastAnswerAt);
            }
        });

        List<RankItem> rank = new ArrayList<RankItem>();
        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            RankItem item = new RankItem();
            item.rank = i + 1;
            item.studentName = p.name;
            item.score = p.score;
            item.correctCount = p.correctCount;
            item.totalAnswered = p.totalAnswered;
            rank.add(item);
        }
        return rank;
    }

    private List<Question> buildQuestions(int questionCount) {
        List<TemplateQuestion> copy;
        synchronized (questionBank) {
            copy = new ArrayList<TemplateQuestion>(questionBank);
        }
        Collections.shuffle(copy, new SecureRandom());

        int count = Math.max(1, Math.min(questionCount, copy.size()));

        List<Question> questions = new ArrayList<Question>();
        for (int i = 0; i < count; i++) {
            TemplateQuestion template = copy.get(i);
            List<String> options = buildOptions(template.correctGroup);

            Question question = new Question();
            question.id = "Q" + (i + 1);
            question.moleculeName = template.moleculeName;
            question.formula = template.formula;
            question.structureHint = template.structureHint;
            question.correctGroup = template.correctGroup;
            question.options = options;
            questions.add(question);
        }
        return questions;
    }

    private List<String> buildOptions(String correct) {
        Set<String> optionSet = new HashSet<String>();
        optionSet.add(correct);

        while (optionSet.size() < OPTIONS_PER_QUESTION) {
            TemplateQuestion randomTemplate = questionBank
                    .get(ThreadLocalRandom.current().nextInt(questionBank.size()));
            optionSet.add(randomTemplate.correctGroup);
        }

        List<String> options = new ArrayList<String>(optionSet);
        Collections.shuffle(options);
        return options;
    }

    private List<TemplateQuestion> buildQuestionBank() {
        List<TemplateQuestion> list = new ArrayList<TemplateQuestion>();
        list.add(new TemplateQuestion("builtin-001", "乙醇", "CH3CH2OH", "末端含 -OH", "羟基(醇)", "醇和酚", false));
        list.add(new TemplateQuestion("builtin-002", "乙酸", "CH3COOH", "末端含 -COOH", "羧基", "羧酸", false));
        list.add(new TemplateQuestion("builtin-003", "乙醛", "CH3CHO", "末端含 -CHO", "醛基", "醛酮", false));
        list.add(new TemplateQuestion("builtin-004", "丙酮", "CH3COCH3", "链中含 >C=O", "酮羰基", "醛酮", false));
        list.add(new TemplateQuestion("builtin-005", "乙酸乙酯", "CH3COOCH2CH3", "含 -COO-", "酯基", "酯", false));
        list.add(new TemplateQuestion("builtin-006", "二甲醚", "CH3OCH3", "含 -O-", "醚键", "醚", false));
        list.add(new TemplateQuestion("builtin-007", "氯乙烷", "CH3CH2Cl", "含 C-Cl", "卤代", "卤代烃", false));
        list.add(new TemplateQuestion("builtin-008", "乙烯", "CH2=CH2", "含 C=C", "碳碳双键", "烯烃", false));
        list.add(new TemplateQuestion("builtin-009", "乙炔", "HC≡CH", "含 C≡C", "碳碳三键", "炔烃", false));
        list.add(new TemplateQuestion("builtin-010", "乙酰胺", "CH3CONH2", "含 -CONH2", "酰胺基", "含氮有机物", false));
        list.add(new TemplateQuestion("builtin-011", "硝基苯", "C6H5NO2", "芳环连 -NO2", "硝基", "含氮有机物", false));
        list.add(new TemplateQuestion("builtin-012", "苯胺", "C6H5NH2", "芳环连 -NH2", "氨基", "含氮有机物", false));
        return list;
    }

    private QuestionTemplateView toTemplateView(TemplateQuestion question) {
        QuestionTemplateView view = new QuestionTemplateView();
        view.id = question.id;
        view.moleculeName = question.moleculeName;
        view.formula = question.formula;
        view.structureHint = question.structureHint;
        view.correctGroup = question.correctGroup;
        view.chapter = question.chapter;
        view.custom = question.custom;
        return view;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String csvEscape(String value) {
        String text = value == null ? "" : value;
        boolean containsSpecial = text.contains(",") || text.contains("\"") || text.contains("\n");
        if (!containsSpecial) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private void loadPersistedCustomQuestions() {
        File file = getCustomQuestionFile();
        if (!file.exists()) {
            return;
        }

        try {
            List<CustomQuestionInput> persisted = objectMapper.readValue(
                    file,
                    new TypeReference<List<CustomQuestionInput>>() {
                    });

            if (persisted == null || persisted.isEmpty()) {
                return;
            }

            int loaded = 0;
            synchronized (questionBank) {
                for (CustomQuestionInput input : persisted) {
                    if (input == null) {
                        continue;
                    }
                    String moleculeName = trim(input.moleculeName);
                    String formula = trim(input.formula);
                    String structureHint = trim(input.structureHint);
                    String correctGroup = trim(input.correctGroup);
                    String chapter = trim(input.chapter);

                    if (moleculeName.isEmpty() || formula.isEmpty() || structureHint.isEmpty()
                            || correctGroup.isEmpty()) {
                        continue;
                    }

                    questionBank.add(new TemplateQuestion(
                            trim(input.id),
                            moleculeName,
                            formula,
                            structureHint,
                            correctGroup,
                            chapter.isEmpty() ? "自定义" : chapter,
                            true));
                    loaded += 1;
                }
            }
            log.info("Loaded {} custom chem race questions from {}", loaded, file.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to load persisted chem race questions from {}", file.getAbsolutePath(), e);
        }
    }

    private void saveCustomQuestions() {
        File file = getCustomQuestionFile();
        File parent = file.getParentFile();

        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("无法创建目录: " + parent.getAbsolutePath());
            }

            List<CustomQuestionInput> persisted = new ArrayList<CustomQuestionInput>();
            synchronized (questionBank) {
                for (TemplateQuestion question : questionBank) {
                    if (!question.custom) {
                        continue;
                    }

                    CustomQuestionInput input = new CustomQuestionInput();
                    input.moleculeName = question.moleculeName;
                    input.id = question.id;
                    input.formula = question.formula;
                    input.structureHint = question.structureHint;
                    input.correctGroup = question.correctGroup;
                    input.chapter = question.chapter;
                    persisted.add(input);
                }
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, persisted);
            log.info("Saved {} custom chem race questions to {}", persisted.size(), file.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to save custom chem race questions to {}", file.getAbsolutePath(), e);
        }
    }

    private File getCustomQuestionFile() {
        return new File(storagePath, "question-bank-custom.json");
    }

    private void loadGlobalStudentAccounts() {
        File file = getGlobalStudentAccountFile();
        if (!file.exists()) {
            return;
        }
        try {
            List<StudentAccountView> persisted = objectMapper.readValue(
                    file,
                    new TypeReference<List<StudentAccountView>>() {
                    });
            if (persisted == null) {
                return;
            }

            synchronized (globalStudentAccounts) {
                globalStudentAccounts.clear();
                for (StudentAccountView view : persisted) {
                    if (view == null) {
                        continue;
                    }
                    String name = trim(view.studentName);
                    String studentId = trim(view.studentId);
                    if (name.isEmpty()) {
                        continue;
                    }
                    StudentAccount account = new StudentAccount();
                    account.studentName = name;
                    account.studentId = studentId;
                    account.displayName = buildDisplayName(name, studentId);
                    globalStudentAccounts.add(account);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load global student accounts from {}", file.getAbsolutePath(), e);
        }
    }

    private void saveGlobalStudentAccounts() {
        File file = getGlobalStudentAccountFile();
        File parent = file.getParentFile();
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("无法创建目录: " + parent.getAbsolutePath());
            }
            synchronized (globalStudentAccounts) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file,
                        toStudentAccountViews(globalStudentAccounts));
            }
        } catch (Exception e) {
            log.warn("Failed to save global student accounts to {}", file.getAbsolutePath(), e);
        }
    }

    private File getGlobalStudentAccountFile() {
        return new File(storagePath, "student-accounts.json");
    }

    private StudentAccount copyStudentAccount(StudentAccount source) {
        StudentAccount copy = new StudentAccount();
        copy.studentName = source.studentName;
        copy.studentId = source.studentId;
        copy.displayName = source.displayName;
        return copy;
    }

    private List<StudentAccountView> toStudentAccountViews(List<StudentAccount> accounts) {
        List<StudentAccountView> views = new ArrayList<StudentAccountView>();
        for (StudentAccount account : accounts) {
            StudentAccountView view = new StudentAccountView();
            view.studentName = account.studentName;
            view.studentId = account.studentId;
            view.displayName = account.displayName;
            views.add(view);
        }
        return views;
    }

    private void saveRaceHistoryIfNeeded(Room room) {
        if (room.historySaved) {
            return;
        }

        RaceHistoryRecord record = new RaceHistoryRecord();
        record.roomCode = room.roomCode;
        record.teacherName = room.teacherName;
        record.finishedAt = System.currentTimeMillis();
        record.totalQuestions = room.questions.size();
        record.leaderboard = new ArrayList<HistoryRankItem>();

        List<RankItem> rankItems = buildLeaderboard(room);
        for (RankItem item : rankItems) {
            if (room.teacherName.equals(item.studentName)) {
                continue;
            }
            HistoryRankItem historyItem = new HistoryRankItem();
            historyItem.rank = item.rank;
            historyItem.studentName = item.studentName;
            historyItem.score = item.score;
            historyItem.correctCount = item.correctCount;
            historyItem.totalAnswered = item.totalAnswered;
            record.leaderboard.add(historyItem);
        }

        synchronized (raceHistory) {
            raceHistory.add(0, record);
            if (raceHistory.size() > MAX_HISTORY_RECORDS) {
                raceHistory.subList(MAX_HISTORY_RECORDS, raceHistory.size()).clear();
            }
        }

        saveRaceHistory();
        room.historySaved = true;
    }

    private void loadRaceHistory() {
        File file = getRaceHistoryFile();
        if (!file.exists()) {
            return;
        }

        try {
            List<RaceHistoryRecord> persisted = objectMapper.readValue(
                    file,
                    new TypeReference<List<RaceHistoryRecord>>() {
                    });
            if (persisted == null || persisted.isEmpty()) {
                return;
            }

            synchronized (raceHistory) {
                raceHistory.clear();
                raceHistory.addAll(persisted);
                if (raceHistory.size() > MAX_HISTORY_RECORDS) {
                    raceHistory.subList(MAX_HISTORY_RECORDS, raceHistory.size()).clear();
                }
            }
            log.info("Loaded {} chem race history records from {}", raceHistory.size(), file.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to load chem race history from {}", file.getAbsolutePath(), e);
        }
    }

    private void saveRaceHistory() {
        File file = getRaceHistoryFile();
        File parent = file.getParentFile();

        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("无法创建目录: " + parent.getAbsolutePath());
            }
            synchronized (raceHistory) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, raceHistory);
            }
        } catch (Exception e) {
            log.warn("Failed to save chem race history to {}", file.getAbsolutePath(), e);
        }
    }

    private File getRaceHistoryFile() {
        return new File(storagePath, "race-history.json");
    }

    private RaceHistoryView toRaceHistoryView(RaceHistoryRecord record) {
        RaceHistoryView view = new RaceHistoryView();
        view.roomCode = record.roomCode;
        view.teacherName = record.teacherName;
        view.finishedAt = record.finishedAt;
        view.totalQuestions = record.totalQuestions;
        view.leaderboard = new ArrayList<HistoryRankItem>();
        if (record.leaderboard != null) {
            for (HistoryRankItem item : record.leaderboard) {
                view.leaderboard.add(copyHistoryItem(item));
            }
        }
        return view;
    }

    private HistoryRankItem copyHistoryItem(HistoryRankItem source) {
        HistoryRankItem item = new HistoryRankItem();
        item.rank = source.rank;
        item.studentName = source.studentName;
        item.score = source.score;
        item.correctCount = source.correctCount;
        item.totalAnswered = source.totalAnswered;
        return item;
    }

    private static class Room {
        String roomCode;
        String teacherName;
        String status = "WAITING";
        List<Question> questions;
        int questionDurationSeconds;
        int questionCount;
        int maxQuestionCount;
        boolean autoNextEnabled;
        int currentQuestionIndex = 0;
        long questionStartedAt = System.currentTimeMillis();
        boolean historySaved = false;
        List<StudentAccount> studentAccounts = new ArrayList<StudentAccount>();
        Map<String, Participant> participants = new HashMap<String, Participant>();
        Map<String, Set<String>> answeredByQuestion = new HashMap<String, Set<String>>();

        Room(String roomCode, String teacherName, List<Question> questions,
                int questionDurationSeconds, boolean autoNextEnabled) {
            this.roomCode = roomCode;
            this.teacherName = teacherName;
            this.questions = questions;
            this.questionCount = questions == null ? 0 : questions.size();
            this.maxQuestionCount = this.questionCount;
            this.questionDurationSeconds = questionDurationSeconds;
            this.autoNextEnabled = autoNextEnabled;
        }
    }

    private static class Participant {
        String name;
        int score;
        int correctCount;
        int totalAnswered;
        long lastAnswerAt;

        Participant(String name) {
            this.name = name;
            this.lastAnswerAt = System.currentTimeMillis();
        }
    }

    private static class TemplateQuestion {
        String id;
        String moleculeName;
        String formula;
        String structureHint;
        String correctGroup;
        String chapter;
        boolean custom;

        TemplateQuestion(String id, String moleculeName, String formula, String structureHint,
                String correctGroup, String chapter, boolean custom) {
            this.id = trimStatic(id);
            if (this.id.isEmpty()) {
                this.id = "custom-" + UUID.randomUUID().toString();
            }
            this.moleculeName = moleculeName;
            this.formula = formula;
            this.structureHint = structureHint;
            this.correctGroup = correctGroup;
            this.chapter = chapter;
            this.custom = custom;
        }

        private static String trimStatic(String value) {
            return value == null ? "" : value.trim();
        }
    }

    private static class StudentAccount {
        String studentName;
        String studentId;
        String displayName;
    }

    public static class Question {
        public String id;
        public String moleculeName;
        public String formula;
        public String structureHint;
        public String correctGroup;
        public List<String> options;
    }

    public static class RoomSnapshot {
        public String roomCode;
        public String teacherName;
        public String status;
        public int currentQuestionIndex;
        public int totalQuestions;
        public int participantCount;
        public int questionDurationSeconds;
        public int questionCount;
        public int maxQuestionCount;
        public int remainingSeconds;
        public boolean autoNextEnabled;
        public Question question;
        public List<StudentAccountView> studentAccounts;
        public List<RankItem> leaderboard;
    }

    public static class AnswerResult {
        public String studentName;
        public String questionId;
        public boolean correct;
        public String correctGroup;
        public int deltaScore;
        public int totalScore;
        public int rank;
    }

    public static class RankItem {
        public int rank;
        public String studentName;
        public int score;
        public int correctCount;
        public int totalAnswered;
    }

    public static class CustomQuestionInput {
        public String id;
        public String moleculeName;
        public String formula;
        public String structureHint;
        public String correctGroup;
        public String chapter;
    }

    public static class QuestionTemplateView {
        public String id;
        public String moleculeName;
        public String formula;
        public String structureHint;
        public String correctGroup;
        public String chapter;
        public boolean custom;
    }

    public static class StudentAccountView {
        public String studentName;
        public String studentId;
        public String displayName;
    }

    public static class CsvExport {
        public String fileName;
        public String content;
    }

    public static class RaceHistoryView {
        public String roomCode;
        public String teacherName;
        public long finishedAt;
        public int totalQuestions;
        public List<HistoryRankItem> leaderboard;
    }

    public static class HistoryRankItem {
        public int rank;
        public String studentName;
        public int score;
        public int correctCount;
        public int totalAnswered;
    }

    public static class RaceHistoryRecord {
        public String roomCode;
        public String teacherName;
        public long finishedAt;
        public int totalQuestions;
        public List<HistoryRankItem> leaderboard;
    }
}
