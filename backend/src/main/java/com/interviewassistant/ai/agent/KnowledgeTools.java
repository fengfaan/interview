package com.interviewassistant.ai.agent;

import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.ObsidianService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeTools {

    private final ObsidianService obsidianService;

    public KnowledgeTools(ObsidianService obsidianService) {
        this.obsidianService = obsidianService;
    }

    @Tool(description = "搜索知识库笔记。在 Obsidian 面试知识库中根据关键词查找相关笔记，返回笔记标题和标签。当候选人追问涉及需要补充资料的具体技术知识点时使用此工具。")
    public String searchNotes(
            @ToolParam(description = "搜索关键词，2-5个字，聚焦于具体技术概念，如'B+树'、'Redis集群'、'CAP定理'") String keyword) {
        if (!obsidianService.isVaultConfigured()) {
            return "知识库未配置，无法搜索。请直接基于现有信息回答。";
        }
        try {
            List<NoteItem> notes = obsidianService.searchNotes(keyword);
            if (notes.isEmpty()) {
                return "未找到与「" + keyword + "」相关的笔记。请直接基于现有信息回答。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(notes.size()).append(" 条相关笔记：\n");
            for (NoteItem note : notes) {
                sb.append("- ").append(note.getTitle());
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    sb.append(" [").append(String.join(", ", note.getTags())).append("]");
                }
                sb.append(" (").append(note.getId()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "搜索知识库时出错：" + e.getMessage() + "。请直接基于现有信息回答。";
        }
    }
}
