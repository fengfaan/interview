package com.interviewassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.common.SseUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BrowserCaptureService captureService;
    private final InterviewAiService interviewService;
    private final ObsidianService obsidianService;
    private final Executor executor;

    public ImportController(BrowserCaptureService captureService,
                            InterviewAiService interviewService,
                            ObsidianService obsidianService,
                            @Qualifier("sseTaskExecutor") Executor executor) {
        this.captureService = captureService;
        this.interviewService = interviewService;
        this.obsidianService = obsidianService;
        this.executor = executor;
    }

    @PostMapping("/capture")
    public ApiResponse<CaptureResponse> capture(@Valid @RequestBody CaptureRequest request) {
        CaptureResponse response = captureService.capture(request.getUrl());
        return ApiResponse.ok(response);
    }

    @PostMapping("/parse")
    public ApiResponse<ParseResponse> parse(@Valid @RequestBody ParseRequest request) {
        List<ParseResponse.ParsedQuestion> items = interviewService.parseWebQuestions(
                request.getContent());
        return ApiResponse.ok(new ParseResponse(items));
    }

    @PostMapping(value = "/parse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter parseStream(@Valid @RequestBody ParseRequest request) {
        SseEmitter emitter = SseUtils.createBatchEmitter();
        AtomicBoolean closed = new AtomicBoolean(false);

        emitter.onTimeout(() -> { closed.set(true); emitter.complete(); });
        emitter.onCompletion(() -> closed.set(true));
        emitter.onError(e -> closed.set(true));

        executor.execute(() -> {
            try {
                List<List<ParseResponse.ParsedQuestion>> chunks =
                        interviewService.parseWebQuestionsChunked(request.getContent());

                for (int i = 0; i < chunks.size(); i++) {
                    if (closed.get()) return;
                    List<ParseResponse.ParsedQuestion> chunk = chunks.get(i);
                    if (chunk.isEmpty()) continue;

                    SseUtils.sendProgress(emitter, "解析段 " + (i + 1) + "/" + chunks.size());

                    String json = OBJECT_MAPPER.writeValueAsString(
                            new ParseResponse(chunk));
                    emitter.send(SseEmitter.event()
                            .name("items")
                            .data(json, MediaType.APPLICATION_JSON));
                }

                if (!closed.get()) {
                    SseUtils.sendDone(emitter);
                }
            } catch (Exception e) {
                if (!closed.get()) {
                    SseUtils.sendError(emitter, "PARSE_ERROR", "解析失败: " + e.getMessage());
                }
            }
        });

        return emitter;
    }

    @PostMapping("/save")
    public ApiResponse<List<ImportSaveResult>> save(@Valid @RequestBody ImportSaveRequest request) {
        List<ImportSaveResult> results = new ArrayList<>();
        for (ParseResponse.ParsedQuestion item : request.getItems()) {
            try {
                CreateNoteRequest noteRequest = new CreateNoteRequest();
                noteRequest.setTitle(item.getQuestion());
                noteRequest.setDirection("网页抓题");
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
