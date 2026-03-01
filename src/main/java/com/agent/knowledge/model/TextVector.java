package com.agent.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量化文本对象
 * 包含原始文本和对应的向量表示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextVector {

    /** 原始文本 */
    private String text;

    /** 文本的向量表示 */
    private double[] vector;

    /** 向量维度 */
    private int dimension;

    /**
     * 计算与另一个向量的余弦相似度
     * 相似度范围：[-1, 1]，值越高表示越相似
     */
    public double cosineSimilarity(TextVector other) {
        if (this.vector == null || other.vector == null) {
            return 0.0;
        }

        if (this.vector.length != other.vector.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < this.vector.length; i++) {
            dotProduct += this.vector[i] * other.vector[i];
            normA += this.vector[i] * this.vector[i];
            normB += other.vector[i] * other.vector[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 计算欧几里得距离
     * 距离越小，两个向量越相似
     */
    public double euclideanDistance(TextVector other) {
        if (this.vector == null || other.vector == null) {
            return Double.MAX_VALUE;
        }

        if (this.vector.length != other.vector.length) {
            return Double.MAX_VALUE;
        }

        double sum = 0.0;
        for (int i = 0; i < this.vector.length; i++) {
            double diff = this.vector[i] - other.vector[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }
}
