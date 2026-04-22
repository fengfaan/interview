package com.interviewassistant.service;

import com.interviewassistant.config.AiConfig;
import com.interviewassistant.dto.resume.AnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
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

    private final AiConfig aiConfig;
    private final PromptService promptService;

    public AnalyzeResponse analyze(String jobDescription, String resume) {
        validateAnalysisInput(jobDescription, resume);

        String userMessage = promptService.render("resume/analyze.md", Map.of(
                "jobDescription", jobDescription,
                "resume", resume
        ));

        var converter = new BeanOutputConverter<>(AnalyzeResponse.class);
        String response = aiConfig.getCurrentChatClient().prompt()
                .system(promptService.load("resume/system.md"))
                .user(userMessage + "\n\n" + converter.getFormat())
                .call()
                .content();

        return converter.convert(response);
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
