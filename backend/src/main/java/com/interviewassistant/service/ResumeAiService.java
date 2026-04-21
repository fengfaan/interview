package com.interviewassistant.service;

import com.interviewassistant.config.AiConfig;
import com.interviewassistant.dto.resume.AnalyzeResponse;
import com.interviewassistant.prompt.ResumePrompts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiService {

    private final AiConfig aiConfig;

    public AnalyzeResponse analyze(String jobDescription, String resume) {
        String userMessage = String.format(ResumePrompts.ANALYZE_PROMPT, jobDescription, resume);

        var converter = new BeanOutputConverter<>(AnalyzeResponse.class);
        String response = aiConfig.getCurrentChatClient().prompt()
                .system(ResumePrompts.SYSTEM_PROMPT)
                .user(userMessage + "\n\n" + converter.getFormat())
                .call()
                .content();

        return converter.convert(response);
    }

    public String buildRewritePrompt(String jobDescription, String resume,
                                      String suggestionTitle, String sourceText) {
        return String.format(ResumePrompts.REWRITE_PROMPT,
                jobDescription, resume, suggestionTitle, sourceText);
    }
}
