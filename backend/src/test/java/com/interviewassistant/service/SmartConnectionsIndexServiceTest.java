package com.interviewassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.ai.embedding.OnnxEmbeddingService;
import com.interviewassistant.dto.knowledge.SmartConnectionSearchResult;
import com.interviewassistant.dto.knowledge.SmartConnectionsIndexStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartConnectionsIndexServiceTest {

    @TempDir
    Path vaultDir;

    @Mock
    private SettingsService settingsService;

    @Mock
    private OnnxEmbeddingService onnxEmbeddingService;

    private SmartConnectionsIndexService service;

    @BeforeEach
    void setUp() {
        service = new SmartConnectionsIndexService(settingsService, new ObjectMapper(), onnxEmbeddingService);
        when(settingsService.getVaultPath()).thenReturn(vaultDir.toString());
    }

    @Test
    void status_readsSmartEnvVectors() throws Exception {
        writeSmartEnvIndex();

        SmartConnectionsIndexStatus status = service.status();

        assertTrue(status.isAvailable());
        assertEquals(3, status.getSourceCount());
        assertEquals(1, status.getBlockCount());
        assertEquals(4, status.getVectorCount());
        assertEquals(List.of("TaylorAI/bge-micro-v2"), status.getModelKeys());
        assertEquals(4, status.getDimensions().get(2));
    }

    @Test
    void findSimilarToNote_usesExistingSmartConnectionsVectors() throws Exception {
        writeSmartEnvIndex();

        List<SmartConnectionSearchResult> results = service.findSimilarToNote("a.md", 2, false);

        assertEquals(2, results.size());
        assertEquals("b.md", results.get(0).getPath());
        assertTrue(results.get(0).getScore() > results.get(1).getScore());
        assertEquals("source", results.get(0).getType());
    }

    @Test
    void findSimilarToNote_canIncludeBlocks() throws Exception {
        writeSmartEnvIndex();

        List<SmartConnectionSearchResult> results = service.findSimilarToNote("a.md", 3, true);

        assertTrue(results.stream().anyMatch(result -> "block".equals(result.getType())));
        assertTrue(results.stream().anyMatch(result -> "a.md".equals(result.getSourcePath())));
    }

    @Test
    void search_embedsQueryAndSearchesSmartConnectionsVectors() throws Exception {
        writeSmartEnvIndex();
        when(onnxEmbeddingService.embed("高并发 WebSocket"))
                .thenReturn(new OnnxEmbeddingService.EmbeddingResult("TaylorAI/bge-micro-v2", new double[]{1.0, 0.0}));

        List<SmartConnectionSearchResult> results = service.search("高并发 WebSocket", 2, false);

        assertEquals(2, results.size());
        assertEquals("a.md", results.get(0).getPath());
        assertEquals("b.md", results.get(1).getPath());
    }

    private void writeSmartEnvIndex() throws Exception {
        Path multiDir = vaultDir.resolve(".smart-env").resolve("multi");
        Files.createDirectories(multiDir);
        String content = """
                "smart_sources:a.md": {"path":"a.md","embeddings":{"TaylorAI/bge-micro-v2":{"vec":[1.0,0.0]}}},
                "smart_sources:b.md": {"path":"b.md","embeddings":{"TaylorAI/bge-micro-v2":{"vec":[0.9,0.1]}}},
                "smart_sources:c.md": {"path":"c.md","embeddings":{"TaylorAI/bge-micro-v2":{"vec":[-1.0,0.0]}}},
                "smart_blocks:a.md##细节": {"path":null,"embeddings":{"TaylorAI/bge-micro-v2":{"vec":[0.8,0.2]}}},
                """;
        Files.writeString(multiDir.resolve("sample.ajson"), content, StandardCharsets.UTF_8);
    }
}
