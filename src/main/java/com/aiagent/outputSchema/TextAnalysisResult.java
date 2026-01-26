package com.aiagent.outputSchema;

import lombok.Data;

import java.util.List;

/**
 * @author jiangtao.shu
 * 用于将 JSON 字符串转换为 Java Bean 的工具类,AI-NLP 输出标准化容器
 * - **`summary`** → 说什么（内容）
 * - **`keywords`** → 关键点是什么（重点）
 * - **`sentiment`** → 态度如何（情感）
 * - **`confidence`** → 有多确定（可信度）
 * - **`style`** → 怎么说的（形式）
 */
@Data
public class TextAnalysisResult {
    private String summary;
    private List<String> keywords;
    private String sentiment;
    private Double confidence;
    private String style;
}
