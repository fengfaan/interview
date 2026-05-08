package com.interviewassistant.dto.resume;

import lombok.Data;
import java.util.List;

@Data
public class StructureAnalysisResponse {
    private int structureScore;
    private List<ModuleCheck> moduleChecks;
    private List<ParagraphIssue> issues;
    private String summary;

    @Data
    public static class ModuleCheck {
        private String name;
        private String status; // pass, warn, fail
        private String detail;
    }

    @Data
    public static class ParagraphIssue {
        private String severity; // critical, warning, info
        private String quote;    // 引用简历中的原文片段
        private String location; // 问题所在位置描述（如"自我评价段落"、"XX公司经历第2条"）
        private String problem;  // 具体问题描述
        private String action;   // 操作类型：删除 / 缩短 / 改写 / 移动 / 补充
        private String suggestion; // 具体怎么改
        private String rewrite;  // 改写后的参考文本（删除类可为空）
    }
}
