package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.import_.CaptureRequest;
import com.interviewassistant.dto.import_.CaptureResponse;
import com.interviewassistant.dto.import_.ImportSaveRequest;
import com.interviewassistant.dto.import_.ImportSaveResult;
import com.interviewassistant.dto.import_.ParseRequest;
import com.interviewassistant.dto.import_.ParseResponse;
import com.interviewassistant.dto.knowledge.CreateNoteRequest;
import com.interviewassistant.service.BrowserCaptureService;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final BrowserCaptureService captureService;
    private final InterviewAiService interviewService;
    private final ObsidianService obsidianService;

    @PostMapping("/capture")
    public ApiResponse<CaptureResponse> capture(@Valid @RequestBody CaptureRequest request) {
        CaptureResponse response = captureService.capture(request.getUrl());
        return ApiResponse.ok(response);
    }

    @PostMapping("/parse")
    public ApiResponse<ParseResponse> parse(@Valid @RequestBody ParseRequest request) {
        List<ParseResponse.ParsedQuestion> items = interviewService.parseWebQuestions(
                request.getDirection(), request.getLevel(), request.getContent());
        return ApiResponse.ok(new ParseResponse(items));
    }

    @PostMapping("/save")
    public ApiResponse<List<ImportSaveResult>> save(@Valid @RequestBody ImportSaveRequest request) {
        List<ImportSaveResult> results = new ArrayList<>();
        for (ParseResponse.ParsedQuestion item : request.getItems()) {
            try {
                CreateNoteRequest noteRequest = new CreateNoteRequest();
                noteRequest.setTitle(item.getQuestion());
                noteRequest.setDirection(request.getDirection());
                noteRequest.setContent("## 题目\n\n" + item.getQuestion()
                        + "\n\n## 参考答案\n\n" + (item.getAnswer() != null ? item.getAnswer() : "暂无答案")
                        + "\n\n## 考察要点\n\n" + String.join("、", item.getKeywords()));
                noteRequest.setTags(item.getKeywords());
                noteRequest.setSource("web-import");
                noteRequest.setUrl(request.getSourceUrl());

                obsidianService.createNote(noteRequest);
                results.add(new ImportSaveResult(item.getQuestion(), true, null));
            } catch (Exception e) {
                log.warn("Failed to save imported question: {}", e.getMessage());
                results.add(new ImportSaveResult(item.getQuestion(), false, e.getMessage()));
            }
        }
        return ApiResponse.ok(results);
    }
}
