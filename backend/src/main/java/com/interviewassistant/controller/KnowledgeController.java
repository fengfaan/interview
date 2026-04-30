package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.knowledge.CreateNoteRequest;
import com.interviewassistant.dto.knowledge.NoteDetail;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.dto.knowledge.SimilarNotesResult;
import com.interviewassistant.dto.knowledge.SmartConnectionSearchResult;
import com.interviewassistant.dto.knowledge.SmartConnectionsIndexStatus;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.SmartConnectionsIndexService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeController {

    private final ObsidianService obsidianService;
    private final SmartConnectionsIndexService smartConnectionsIndexService;

    @GetMapping("/notes")
    public ApiResponse<List<NoteItem>> listNotes(
            @RequestParam(value = "direction", required = false) String direction) {
        if (!obsidianService.isVaultConfigured()) {
            return ApiResponse.fail("VAULT_NOT_CONFIGURED", "Obsidian Vault 未配置，请先在设置页配置路径");
        }
        return ApiResponse.ok(obsidianService.listNotes(direction));
    }

    @GetMapping("/note")
    public ApiResponse<NoteDetail> getNote(@RequestParam("id") String noteId) {
        if (!obsidianService.isVaultConfigured()) {
            return ApiResponse.fail("VAULT_NOT_CONFIGURED", "Obsidian Vault 未配置，请先在设置页配置路径");
        }
        try {
            return ApiResponse.ok(obsidianService.readNote(noteId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("NOTE_NOT_FOUND", e.getMessage());
        }
    }

    @PostMapping("/notes")
    public ApiResponse<?> createNote(@Valid @RequestBody CreateNoteRequest request) {
        if (!obsidianService.isVaultConfigured()) {
            return ApiResponse.fail("VAULT_NOT_CONFIGURED", "Obsidian Vault 未配置，请先在设置页配置路径");
        }
        try {
            boolean force = Boolean.TRUE.equals(request.getForce());
            if (!force) {
                SimilarNotesResult similar = obsidianService.findSimilarNotes(request.getTitle(), request.getDirection());
                if (!similar.getNotes().isEmpty()) {
                    String method = "vector".equals(similar.getSearchMethod()) ? "向量搜索" : "文本匹配";
                    return ApiResponse.fail("DUPLICATE_FOUND", "通过" + method + "发现相似笔记", similar);
                }
            }
            return ApiResponse.ok(obsidianService.createNote(request));
        } catch (Exception e) {
            log.error("Failed to create note", e);
            return ApiResponse.fail("CREATE_FAILED", "保存笔记失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<List<NoteItem>> searchNotes(@RequestParam("keyword") String keyword) {
        if (!obsidianService.isVaultConfigured()) {
            return ApiResponse.fail("VAULT_NOT_CONFIGURED", "Obsidian Vault 未配置，请先在设置页配置路径");
        }
        return ApiResponse.ok(obsidianService.searchNotes(keyword));
    }

    @GetMapping("/smart-connections/status")
    public ApiResponse<SmartConnectionsIndexStatus> smartConnectionsStatus() {
        return ApiResponse.ok(smartConnectionsIndexService.status());
    }

    @GetMapping("/smart-connections/similar")
    public ApiResponse<List<SmartConnectionSearchResult>> findSimilarBySmartConnectionsVector(
            @RequestParam("id") String noteId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "includeBlocks", defaultValue = "false") boolean includeBlocks) {
        return ApiResponse.ok(smartConnectionsIndexService.findSimilarToNote(noteId, limit, includeBlocks));
    }

    @GetMapping("/smart-connections/search")
    public ApiResponse<List<SmartConnectionSearchResult>> searchBySmartConnectionsVector(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "includeBlocks", defaultValue = "true") boolean includeBlocks) {
        try {
            return ApiResponse.ok(smartConnectionsIndexService.search(query, limit, includeBlocks));
        } catch (Exception e) {
            log.warn("Smart Connections vector search failed", e);
            return ApiResponse.fail("SMART_EMBEDDING_UNAVAILABLE", e.getMessage());
        }
    }
}
