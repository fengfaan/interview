package com.interviewassistant.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.*;

public class BertTokenizer {

    private final Map<String, Integer> vocab;
    private final boolean doLowerCase;
    private final int clsTokenId;
    private final int sepTokenId;

    public BertTokenizer(String tokenizerPath, boolean doLowerCase) {
        this.doLowerCase = doLowerCase;
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is;
            if (tokenizerPath.startsWith("classpath:")) {
                is = new ClassPathResource(tokenizerPath.substring(10)).getInputStream();
            } else {
                is = new java.io.FileInputStream(tokenizerPath);
            }
            JsonNode root = mapper.readTree(is);

            Map<String, Integer> v = new HashMap<>();
            JsonNode vocabNode = root.path("model").path("vocab");
            Iterator<Map.Entry<String, JsonNode>> fields = vocabNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                v.put(entry.getKey(), entry.getValue().asInt());
            }
            this.vocab = Collections.unmodifiableMap(v);
            this.clsTokenId = v.getOrDefault("[CLS]", 101);
            this.sepTokenId = v.getOrDefault("[SEP]", 102);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer: " + e.getMessage(), e);
        }
    }

    public record Tokenization(long[] inputIds, long[] attentionMask) {}

    public Tokenization tokenize(String text, int maxLength) {
        String processed = doLowerCase ? text.toLowerCase(Locale.ROOT) : text;
        List<String> tokens = basicTokenize(processed);
        List<String> wordPieces = new ArrayList<>();
        for (String token : tokens) {
            wordPieceTokenize(token, wordPieces);
        }

        int maxTokens = maxLength - 2;
        if (wordPieces.size() > maxTokens) {
            wordPieces = wordPieces.subList(0, maxTokens);
        }

        int seqLen = wordPieces.size() + 2;
        long[] inputIds = new long[seqLen];
        long[] attentionMask = new long[seqLen];

        inputIds[0] = clsTokenId;
        for (int i = 0; i < wordPieces.size(); i++) {
            inputIds[i + 1] = vocab.getOrDefault(wordPieces.get(i), vocab.getOrDefault("[UNK]", 100));
        }
        inputIds[wordPieces.size() + 1] = sepTokenId;
        Arrays.fill(attentionMask, 0, seqLen, 1L);

        return new Tokenization(inputIds, attentionMask);
    }

    private List<String> basicTokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (isPunctuation(c)) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    private void wordPieceTokenize(String word, List<String> output) {
        if (vocab.containsKey(word)) {
            output.add(word);
            return;
        }
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < word.length()) {
            int end = word.length();
            boolean found = false;
            while (end > start) {
                String sub = (start > 0 ? "##" : "") + word.substring(start, end);
                if (vocab.containsKey(sub)) {
                    pieces.add(sub);
                    start = end;
                    found = true;
                    break;
                }
                end--;
            }
            if (!found) {
                pieces.add("[UNK]");
                start++;
            }
        }
        output.addAll(pieces);
    }

    private boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.START_PUNCTUATION;
    }
}
