package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.dto.resume.StructureAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeAiServiceTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private PromptService promptService;

    @InjectMocks
    private ResumeAiService resumeAiService;

    @Test
    void analyzeStructure_shouldThrowExceptionWhenResumeTooShort() {
        assertThrows(IllegalArgumentException.class, 
            () -> resumeAiService.analyzeStructure("123"));
    }

    @Test
    void analyzeStructure_shouldThrowExceptionWhenNoResumeSignals() {
        String noSignalResume = "这只是一段没有任何关键词的普通文字，字数肯定是超过五十个字了的，这只是一段没有任何关键" +
                "词的普通文字，这只是一段没有任何关键词的普通文字，这只是一段没有任何关键词的普通文字。";
        assertThrows(IllegalArgumentException.class, 
            () -> resumeAiService.analyzeStructure(noSignalResume));
    }

    @Test
    void analyzeStructure_shouldReturnAnalysisResponse() {
        String validResume = "这是一段很长的测试简历文本，包含了项目经验、负责开发、Java 技能等关键词，" +
                "字数也肯定超过五十个有意义的字符了，可以用来测试我们这个结构化分析的功能，工作，教育等。";
                
        String renderedPrompt = "rendered_prompt";
        when(promptService.render(eq("resume/structure-analysis.md"), any(Map.class)))
                .thenReturn(renderedPrompt);
                
        StructureAnalysisResponse mockResponse = new StructureAnalysisResponse();
        mockResponse.setStructureScore(85);
        AiGateway.JsonResult<StructureAnalysisResponse> mockResult = 
            new AiGateway.JsonResult<>(mockResponse, "test-model");
            
        when(aiGateway.generateJson(anyString(), eq(renderedPrompt), eq(StructureAnalysisResponse.class)))
                .thenReturn(mockResult);

        AiGateway.JsonResult<StructureAnalysisResponse> result = resumeAiService.analyzeStructure(validResume);

        assertEquals(85, result.value().getStructureScore());
        assertEquals("test-model", result.actualModel());
        
        verify(aiGateway).generateJson(
                eq("请严格按照 JSON 格式输出简历结构诊断报告。"),
                eq(renderedPrompt),
                eq(StructureAnalysisResponse.class)
        );
    }
}
