package com.agent.knowledge.service;

import com.agent.knowledge.model.TextVector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.*;
import java.util.*;

/**
 * 文本嵌入服务
 * 将文本转换为向量表示，用于相似度计算
 */
@Slf4j
@Component
public class EmbeddingService {

    // 词汇表 - 用于固定维度的向量表示
    private static final Set<String> VOCABULARY = new HashSet<>();
    private static final int VECTOR_DIMENSION = 100;
    private static final Random RANDOM = new Random(42); // 固定种子保证一致性

    static {
        // 初始化词汇表（常见中文和英文词）
        String[] words = {
                "你好", "谢谢", "对不起", "请", "我", "他", "她", "它",
                "是", "不是", "有", "没有", "好", "坏", "大", "小",
                "快", "慢", "多", "少", "对", "错", "yes", "no",
                "hello", "thanks", "sorry", "please", "help", "information",
                "document", "knowledge", "base", "search", "find", "get",
                "what", "how", "why", "when", "where", "who",
                "医疗", "编程", "学习", "工作", "生活", "技术",
                "java", "python", "spring", "boot", "database", "api"
        };
        Collections.addAll(VOCABULARY, words);
    }

    /**
     * 生成文本的向量表示
     * 使用基于词频和词语的简单算法
     */
    public TextVector embed(String text) {
        if (text == null || text.isEmpty()) {
            return new TextVector(text, new double[VECTOR_DIMENSION], VECTOR_DIMENSION);
        }

        double[] vector = new double[VECTOR_DIMENSION];

        // 分词（简单分割）
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fff]", " ")
                .split("\\s+");

        // 词频统计
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }

        // 生成向量
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            double value = 0.0;

            // 对每个词的贡献进行加权
            for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
                String word = entry.getKey();
                int freq = entry.getValue();

                // 使用词的哈希值和向量维度组合计算该词对该维度的贡献
                double contribution = (double) (word.hashCode() ^ i) % 100 / 100.0 * freq;
                value += contribution;
            }

            // 归一化
            vector[i] = value / Math.max(1, words.length);
        }

        // 向量正则化
        normalizeVector(vector);

        return new TextVector(text, vector, VECTOR_DIMENSION);
    }

    /**
     * 向量正则化（L2归一化）
     */
    private void normalizeVector(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    /**
     * 批量生成向量
     */
    public List<TextVector> embedBatch(List<String> texts) {
        List<TextVector> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        log.info("✅ Embedded {} texts", vectors.size());
        return vectors;
    }

    /**
     * 获取向量维度
     */
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }
}
