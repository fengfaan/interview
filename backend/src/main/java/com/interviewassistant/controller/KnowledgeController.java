package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.knowledge.CreateNoteRequest;
import com.interviewassistant.dto.knowledge.NoteDetail;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.ObsidianService;
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
    public ApiResponse<NoteItem> createNote(@Valid @RequestBody CreateNoteRequest request) {
        if (!obsidianService.isVaultConfigured()) {
            return ApiResponse.fail("VAULT_NOT_CONFIGURED", "Obsidian Vault 未配置，请先在设置页配置路径");
        }
        try {
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
}
