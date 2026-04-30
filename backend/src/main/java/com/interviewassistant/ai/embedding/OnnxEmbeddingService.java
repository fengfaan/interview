package com.interviewassistant.ai.embedding;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxTensor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Slf4j
@Service
public class OnnxEmbeddingService {

    private static final String MODEL_RESOURCE = "embedding/model_quantized.onnx";
    private static final String TOKENIZER_RESOURCE = "embedding/tokenizer.json";
    private static final int MAX_SEQ_LENGTH = 512;
    private static final String MODEL_NAME = "TaylorAI/bge-micro-v2";
    private static final int EXPECTED_DIMS = 384;

    private OrtEnvironment env;
    private OrtSession session;
    private BertTokenizer tokenizer;

    @PostConstruct
    void init() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            Path modelPath = extractResourceToTemp(MODEL_RESOURCE, "embedding-model", ".onnx");
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            this.session = env.createSession(modelPath.toString(), opts);
            this.tokenizer = new BertTokenizer("classpath:" + TOKENIZER_RESOURCE, true);
            log.info("ONNX embedding model loaded: dims={}, model={}", EXPECTED_DIMS, MODEL_NAME);
        } catch (Exception e) {
            log.error("Failed to load ONNX embedding model: {}", e.getMessage());
            throw new RuntimeException("ONNX embedding model 加载失败: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e) {
            log.warn("Error closing ONNX session: {}", e.getMessage());
        }
    }

    public record EmbeddingResult(String model, double[] vector) {}

    public EmbeddingResult embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("查询文本不能为空");
        }
        try {
            BertTokenizer.Tokenization tok = tokenizer.tokenize(text, MAX_SEQ_LENGTH);
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, new long[][]{tok.inputIds()});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, new long[][]{tok.attentionMask()});
            Map<String, OnnxTensor> inputs = Map.of(
                    "input_ids", inputIdsTensor,
                    "attention_mask", attentionMaskTensor
            );
            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                double[] pooled = meanPool(output[0], tok.attentionMask());
                double[] normalized = normalize(pooled);
                return new EmbeddingResult(MODEL_NAME, round(normalized));
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference 失败: " + e.getMessage(), e);
        }
    }

    private double[] meanPool(float[][] tokenVectors, long[] attentionMask) {
        int dims = tokenVectors[0].length;
        double[] result = new double[dims];
        double count = 0;
        for (int i = 0; i < tokenVectors.length; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < dims; j++) {
                    result[j] += tokenVectors[i][j];
                }
                count++;
            }
        }
        if (count > 0) {
            for (int j = 0; j < dims; j++) {
                result[j] /= count;
            }
        }
        return result;
    }

    private double[] normalize(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) return vector;
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] / norm;
        }
        return result;
    }

    private double[] round(double[] vector) {
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = Math.round(vector[i] * 1e8) / 1e8;
        }
        return result;
    }

    private Path extractResourceToTemp(String resourcePath, String prefix, String suffix) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            Path tempFile = Files.createTempFile(prefix, suffix);
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }
    }
}
