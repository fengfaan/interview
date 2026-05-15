package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.dto.resume.AnalyzeResponse;
import com.interviewassistant.dto.resume.HealthCheckupResponse;
import com.interviewassistant.dto.resume.StructureAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiService {

    private static final int MIN_JD_MEANINGFUL_CHARS = 30;
    private static final int MIN_RESUME_MEANINGFUL_CHARS = 50;

    private static final String[] JD_SIGNALS = {
            "岗位", "职位", "职责", "要求", "任职", "招聘", "经验", "熟悉", "负责", "优先",
            "Java", "Spring", "Vue", "React", "Go", "Python", "MySQL", "Redis", "架构", "系统"
    };

    private static final String[] RESUME_SIGNALS = {
            "项目", "工作", "经历", "经验", "技能", "教育", "公司", "负责", "开发", "优化",
            "Java", "Spring", "Vue", "React", "Go", "Python", "MySQL", "Redis", "架构", "系统"
    };

    private final AiGateway aiGateway;
    private final PromptService promptService;

    public AnalyzeResponse analyze(String jobDescription, String resume) {
        validateAnalysisInput(jobDescription, resume);

        String userMessage = promptService.render("resume/analyze.md", Map.of(
                "jobDescription", jobDescription,
                "resume", resume
        ));

        return aiGateway.generateJson(
                promptService.load("resume/system.md"), userMessage, AnalyzeResponse.class).value();
    }

    public String buildRewritePrompt(String jobDescription, String resume,
                                      String suggestionTitle, String sourceText) {
        return promptService.render("resume/rewrite.md", Map.of(
                "jobDescription", jobDescription,
                "resume", resume,
                "suggestionTitle", suggestionTitle != null ? suggestionTitle : "",
                "sourceText", sourceText != null ? sourceText : ""
        ));
    }

    public AiGateway.JsonResult<HealthCheckupResponse> healthCheckup(String resume, String jobDescription) {
        if (resume == null || resume.replaceAll("\\s", "").length() < MIN_RESUME_MEANINGFUL_CHARS) {
            throw new IllegalArgumentException("简历内容过短，请提供完整的简历");
        }
        if (!containsAny(resume, RESUME_SIGNALS)) {
            throw new IllegalArgumentException("简历缺少项目、经历、技能或技术关键词，暂时无法进行有效分析");
        }

        boolean hasJd = jobDescription != null && !jobDescription.isBlank();
        String jdSection = hasJd ? "## 目标职位 JD\n" + jobDescription : "";
        String atsSection = hasJd
                ? "## 第1关：ATS过筛率（0-100）\n评估标准：\n"
                  + "- 从JD中提取核心技术关键词（框架、语言、工具、平台），检查是否在简历中**原样出现**\n"
                  + "- 统计关键词覆盖率：出现数/总数\n"
                  + "- Section标题是否标准（工作经历/项目经历/教育背景/专业技能/自我评价等）\n"
                  + "- 日期格式是否统一可解析（如 2022.03-2024.05 或 2022年3月-2024年5月）\n"
                  + "- 是否有清晰的section划分结构"
                : "## 第1关：ATS过筛率\n未提供JD，此维度跳过。输出：{ \"score\": null, \"detail\": \"未提供JD，ATS关键词匹配已跳过\", \"skipped\": true }";

        String systemPrompt = "请严格按照 JSON 格式输出简历体检报告。";
        String prompt = promptService.render("resume/health-checkup.md", Map.of(
                "resume", resume,
                "jobDescriptionSection", jdSection,
                "atsSection", atsSection
        ));

        return aiGateway.generateJson(systemPrompt, prompt, HealthCheckupResponse.class);
    }

    public AiGateway.JsonResult<StructureAnalysisResponse> analyzeStructure(String resume) {
        if (resume == null || resume.replaceAll("\\s", "").length() < 50) {
            throw new IllegalArgumentException("简历内容过短，请提供完整的简历");
        }
        if (!containsAny(resume, RESUME_SIGNALS)) {
            throw new IllegalArgumentException("简历缺少项目、经历、技能或技术关键词，暂时无法进行有效匹配");
        }

        String systemPrompt = "请严格按照 JSON 格式输出简历结构诊断报告。";
        String prompt = promptService.render("resume/structure-analysis.md",
            Map.of("resume", resume));

        return aiGateway.generateJson(systemPrompt, prompt, StructureAnalysisResponse.class);
    }

    private void validateAnalysisInput(String jobDescription, String resume) {
        int jdLength = meaningfulLength(jobDescription);
        int resumeLength = meaningfulLength(resume);

        if (jdLength < MIN_JD_MEANINGFUL_CHARS) {
            throw new IllegalArgumentException("JD 内容太少，请粘贴至少一段完整的岗位职责或任职要求");
        }
        if (resumeLength < MIN_RESUME_MEANINGFUL_CHARS) {
            throw new IllegalArgumentException("简历内容太少，请至少填写一段包含项目、经历或技能的简历内容");
        }
        if (!containsAny(jobDescription, JD_SIGNALS)) {
            throw new IllegalArgumentException("JD 缺少岗位职责、任职要求或技术关键词，暂时无法进行有效匹配");
        }
        if (!containsAny(resume, RESUME_SIGNALS)) {
            throw new IllegalArgumentException("简历缺少项目、经历、技能或技术关键词，暂时无法进行有效匹配");
        }
    }

    private int meaningfulLength(String text) {
        if (text == null) {
            return 0;
        }
        return (int) text.codePoints()
                .filter(Character::isLetterOrDigit)
                .count();
    }

    private boolean containsAny(String text, String[] signals) {
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase();
        for (String signal : signals) {
            if (normalized.contains(signal.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
