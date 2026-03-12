package com.agent.controller;

import com.agent.service.ChemRaceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 官能团识别课堂竞赛 API。
 */
@RestController
@RequestMapping("/api/chem-race")
public class ChemRaceController {

    private final ChemRaceService chemRaceService;

    public ChemRaceController(ChemRaceService chemRaceService) {
        this.chemRaceService = chemRaceService;
    }

    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest request) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.createRoom(request.teacherName);
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String joinUrl = baseUrl + "/functional-group-race.html?room=" + snapshot.roomCode;

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("joinUrl", joinUrl);
            response.put("message", "房间创建成功，可将链接发给学生参与比赛");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/join")
    public ResponseEntity<Map<String, Object>> joinRoom(@PathVariable String roomCode,
            @RequestBody JoinRequest request) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.joinRoom(
                    roomCode,
                    request.studentName,
                    request.studentId);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "加入成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/start")
    public ResponseEntity<Map<String, Object>> startRace(@PathVariable String roomCode) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.startRace(roomCode);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "比赛已开始");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/next")
    public ResponseEntity<Map<String, Object>> nextQuestion(@PathVariable String roomCode) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.nextQuestion(roomCode);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "已切换下一题");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(@PathVariable String roomCode,
            @RequestBody AnswerRequest request) {
        try {
            ChemRaceService.AnswerResult answerResult = chemRaceService.submitAnswer(
                    roomCode,
                    request.studentName,
                    request.selectedGroup);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("result", answerResult);
            response.put("message", answerResult.correct ? "回答正确" : "回答错误");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomCode}/state")
    public ResponseEntity<Map<String, Object>> getState(@PathVariable String roomCode) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.getState(roomCode);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/settings")
    public ResponseEntity<Map<String, Object>> updateRoomSettings(@PathVariable String roomCode,
            @RequestBody RoomSettingsRequest request) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.updateRoomSettings(
                    roomCode,
                    request.autoNextEnabled,
                    request.questionDurationSeconds,
                    request.questionCount);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "设置已更新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomCode}/leaderboard")
    public ResponseEntity<Map<String, Object>> getLeaderboard(@PathVariable String roomCode) {
        try {
            List<ChemRaceService.RankItem> rankItems = chemRaceService.getLeaderboard(roomCode);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("leaderboard", rankItems);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomCode}/leaderboard/csv")
    public ResponseEntity<String> exportLeaderboardCsv(@PathVariable String roomCode) {
        try {
            ChemRaceService.CsvExport csv = chemRaceService.exportLeaderboardCsv(roomCode);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + csv.fileName + "\"")
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .body(csv.content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("error," + e.getMessage());
        }
    }

    @GetMapping("/question-bank")
    public ResponseEntity<Map<String, Object>> getQuestionBank() {
        List<ChemRaceService.QuestionTemplateView> list = chemRaceService.getQuestionBank();
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("total", list.size());
        response.put("questions", list);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/question-bank/import")
    public ResponseEntity<Map<String, Object>> importQuestions(@RequestBody ImportQuestionsRequest request) {
        try {
            int added = chemRaceService.addCustomQuestions(request.questions);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("added", added);
            response.put("message", "题库导入成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/question-bank/{questionId}")
    public ResponseEntity<Map<String, Object>> updateQuestion(@PathVariable String questionId,
            @RequestBody ChemRaceService.CustomQuestionInput request) {
        try {
            ChemRaceService.QuestionTemplateView question = chemRaceService.updateCustomQuestion(questionId, request);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("question", question);
            response.put("message", "题目修改成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/question-bank/{questionId}")
    public ResponseEntity<Map<String, Object>> deleteQuestion(@PathVariable String questionId) {
        try {
            boolean deleted = chemRaceService.deleteCustomQuestion(questionId);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("deleted", deleted);
            response.put("message", deleted ? "题目删除成功" : "题目不存在");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/finish")
    public ResponseEntity<Map<String, Object>> finishRace(@PathVariable String roomCode) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.finishRace(roomCode);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "比赛已结束");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomCode}/students")
    public ResponseEntity<Map<String, Object>> addStudentAccount(@PathVariable String roomCode,
            @RequestBody StudentAccountRequest request) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.addRoomStudentAccount(
                    roomCode,
                    request.studentName,
                    request.studentId);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "学生账号添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/rooms/{roomCode}/students")
    public ResponseEntity<Map<String, Object>> removeStudentAccount(@PathVariable String roomCode,
            @RequestBody StudentAccountRequest request) {
        try {
            ChemRaceService.RoomSnapshot snapshot = chemRaceService.removeRoomStudentAccount(
                    roomCode,
                    request.studentName,
                    request.studentId);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("room", snapshot);
            response.put("message", "学生账号已移除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/students/global")
    public ResponseEntity<Map<String, Object>> getGlobalStudentAccounts() {
        List<ChemRaceService.StudentAccountView> students = chemRaceService.getGlobalStudentAccounts();
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("total", students.size());
        response.put("students", students);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/students/global")
    public ResponseEntity<Map<String, Object>> addGlobalStudentAccount(@RequestBody StudentAccountRequest request) {
        try {
            List<ChemRaceService.StudentAccountView> students = chemRaceService.addGlobalStudentAccount(
                    request.studentName,
                    request.studentId);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("total", students.size());
            response.put("students", students);
            response.put("message", "学生账号已加入全局名单");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/students/global")
    public ResponseEntity<Map<String, Object>> removeGlobalStudentAccount(@RequestBody StudentAccountRequest request) {
        try {
            List<ChemRaceService.StudentAccountView> students = chemRaceService.removeGlobalStudentAccount(
                    request.studentName,
                    request.studentId);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("total", students.size());
            response.put("students", students);
            response.put("message", "学生账号已从全局名单移除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getRaceHistory() {
        List<ChemRaceService.RaceHistoryView> history = chemRaceService.getRaceHistory();
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("total", history.size());
        response.put("history", history);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/student/{studentName}")
    public ResponseEntity<Map<String, Object>> getStudentHistory(@PathVariable String studentName) {
        try {
            List<ChemRaceService.RaceHistoryView> history = chemRaceService.getStudentHistory(studentName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("studentName", studentName);
            response.put("total", history.size());
            response.put("history", history);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<String, Object>();
        error.put("error", message);
        return ResponseEntity.badRequest().body(error);
    }

    public static class CreateRoomRequest {
        public String teacherName;
    }

    public static class JoinRequest {
        public String studentName;
        public String studentId;
    }

    public static class AnswerRequest {
        public String studentName;
        public String selectedGroup;
    }

    public static class ImportQuestionsRequest {
        public List<ChemRaceService.CustomQuestionInput> questions;
    }

    public static class RoomSettingsRequest {
        public Boolean autoNextEnabled;
        public Integer questionDurationSeconds;
        public Integer questionCount;
    }

    public static class StudentAccountRequest {
        public String studentName;
        public String studentId;
    }
}
