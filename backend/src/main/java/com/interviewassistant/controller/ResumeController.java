package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.resume.AnalyzeRequest;
import com.interviewassistant.dto.resume.AnalyzeResponse;
import com.interviewassistant.dto.resume.ImportFileResponse;
import com.interviewassistant.dto.resume.PolishStreamRequest;
import com.interviewassistant.dto.resume.RewriteStreamRequest;
import com.interviewassistant.dto.resume.StructureAnalysisRequest;
import com.interviewassistant.dto.resume.StructureAnalysisResponse;
import com.interviewassistant.service.ResumeAiService;
import com.interviewassistant.service.ResumeFileParser;
import com.interviewassistant.service.ResumeStreamService;
import com.interviewassistant.ai.gateway.AiGateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeAiService resumeService;
    private final ResumeStreamService resumeStreamService;
    private final ResumeFileParser fileParser;

    @PostMapping("/analyze")
    public ApiResponse<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        AnalyzeResponse response = resumeService.analyze(
                request.getJobDescription(), request.getResume());
        return ApiResponse.ok(response);
    }

    @PostMapping(value = "/rewrite/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRewrite(@Valid @RequestBody RewriteStreamRequest request) {
        return resumeStreamService.streamRewrite(request);
    }

    @PostMapping("/structure-analysis")
    public ApiResponse<AiGateway.JsonResult<StructureAnalysisResponse>> analyzeStructure(@Valid @RequestBody StructureAnalysisRequest request) {
        AiGateway.JsonResult<StructureAnalysisResponse> result = resumeService.analyzeStructure(request.getResume());
        return ApiResponse.ok(result);
    }

    @PostMapping(value = "/polish/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPolish(@Valid @RequestBody PolishStreamRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        resumeStreamService.streamPolish(emitter, request);
        return emitter;
    }

    @PostMapping("/import-file")
    public ApiResponse<ImportFileResponse> importFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("无法识别文件名");
        }
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 PDF 和 DOCX 文件");
        }
        try {
            ResumeFileParser.ParsedResult result = fileParser.parse(file.getBytes(), originalFilename);
            return ApiResponse.ok(new ImportFileResponse(
                    result.text(), originalFilename, result.pageCount(), result.warning()));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + e.getMessage(), e);
        }
    }
}
