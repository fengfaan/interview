package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.resume.AnalyzeRequest;
import com.interviewassistant.dto.resume.AnalyzeResponse;
import com.interviewassistant.dto.resume.RewriteStreamRequest;
import com.interviewassistant.service.ResumeAiService;
import com.interviewassistant.service.ResumeStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeAiService resumeService;
    private final ResumeStreamService resumeStreamService;

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
}
