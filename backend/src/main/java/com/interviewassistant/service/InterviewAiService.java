package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.ai.style.StyleService;
import com.interviewassistant.ai.util.AiErrorUtils;
import com.interviewassistant.ai.util.JsonOutputUtils;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.dto.import_.ParseResponse;
import com.interviewassistant.ai.prompt.InterviewLabels;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final StyleService styleService;
    private final ObjectMapper objectMapper;
    private final AtomicInteger questionCounter = new AtomicInteger(1);
    private static final int BATCH_SIZE = 5;
    private static final int MAX_BATCH_ATTEMPTS = 3;
    private static final long INITIAL_BATCH_RETRY_DELAY_MS = 1_200L;
    private static final int MAX_DEEP_DIVE_CONTEXT_CHARS = 4_000;
    private static final int DEEP_DIVE_EXCERPT_CHARS = 2_800;
    private static final int DEEP_DIVE_OPENING_CHARS = 800;
    private static final int MAX_DEEP_DIVE_MESSAGES = 12;
    private static final int MAX_DEEP_DIVE_MESSAGE_CHARS = 1_200;

    private record DeepDiveSegment(int index, String text, int score) {
    }

    public QuestionResponse generateQuestion(InterviewDirection direction, InterviewLevel level, List<HistoryEntry> history) {
        String historySummary = history == null || history.isEmpty() ? "无（首次提问）"
                : history.stream()
                .map(h -> "Q: " + h.getQuestion() + (h.isSkipped() ? " (已跳过)" : ""))
                .collect(Collectors.joining("\n"));

        String userMessage = promptService.render("interview/question.md", Map.ofEntries(
                Map.entry("direction", InterviewLabels.directionLabel(direction)),
                Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                Map.entry("history", historySummary),
                Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
        ));

        AiGateway.JsonResult<QuestionAiResponse> result = aiGateway.generateJson(
                promptService.load("interview/system.md"), userMessage, QuestionAiResponse.class);
        QuestionAiResponse aiResult = result.value();
        if (aiResult.getQuestionId() == null) {
            aiResult.setQuestionId("q_" + String.format("%03d", questionCounter.getAndIncrement()));
        }
        return new QuestionResponse(
                aiResult.getQuestionId(),
                aiResult.getQuestion(),
                aiResult.getExpectedKeywords(),
                result.actualModel()
        );
    }

    public FeedbackResponse analyzeFeedback(InterviewDirection direction, InterviewLevel level, String question,
                                             String answer, List<String> expectedKeywords) {
        String userMessage = promptService.render("interview/feedback-json.md", Map.ofEntries(
                Map.entry("direction", InterviewLabels.directionLabel(direction)),
                Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                Map.entry("question", question),
                Map.entry("answer", answer),
                Map.entry("expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()),
                Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
        ));

        return aiGateway.generateJson(
                promptService.load("interview/system.md"), userMessage, FeedbackResponse.class).value();
    }

    public String buildFeedbackStreamPrompt(InterviewDirection direction, InterviewLevel level, String question,
                                             String answer, List<String> expectedKeywords,
                                             String followUpQuestion) {
        return promptService.render("interview/feedback-stream.md", Map.ofEntries(
                Map.entry("direction", InterviewLabels.directionLabel(direction)),
                Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                Map.entry("question", question),
                Map.entry("answer", answer),
                Map.entry("expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()),
                Map.entry("followUpQuestion", followUpQuestion != null ? followUpQuestion : "（请基于用户回答生成一个相关的进阶问题）"),
                Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
        ));
    }

    public String buildRecommendedAnswerPrompt(InterviewDirection direction, InterviewLevel level, String question,
                                               List<String> expectedKeywords) {
        return promptService.render("interview/recommended-answer.md", Map.ofEntries(
                Map.entry("direction", InterviewLabels.directionLabel(direction)),
                Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                Map.entry("question", question),
                Map.entry("expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()),
                Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
        ));
    }

    public String buildBatchAnswerPrompt(InterviewDirection direction, InterviewLevel level, String question,
                                          List<String> expectedKeywords) {
        return promptService.render("interview/batch-answer.md", Map.ofEntries(
                Map.entry("direction", InterviewLabels.directionLabel(direction)),
                Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                Map.entry("question", question),
                Map.entry("keywords", expectedKeywords != null ? expectedKeywords : List.of()),
                Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
        ));
    }

    public List<ParseResponse.ParsedQuestion> parseWebQuestions(String content) {
        String userMessage = promptService.render("import/import-parse.md", Map.of(
                "content", content
        ));

        String response = aiGateway.generateText(
                promptService.load("interview/system.md"), userMessage);

        return parseImportResponse(response);
    }

    public List<List<ParseResponse.ParsedQuestion>> parseWebQuestionsChunked(String content) {
        int chunkSize = 4000;
        List<String> chunks = splitIntoChunks(content, chunkSize);
        List<List<ParseResponse.ParsedQuestion>> results = new ArrayList<>();
        for (String chunk : chunks) {
            try {
                results.add(parseWebQuestions(chunk));
            } catch (Exception e) {
                log.warn("Failed to parse chunk, skipping: {}", e.getMessage());
            }
        }
        return results;
    }

    private List<String> splitIntoChunks(String text, int chunkSize) {
        if (text.length() <= chunkSize) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start = end;
            while (start < text.length() && text.charAt(start) == '\n') {
                start++;
            }
        }
        return chunks;
    }

    private List<ParseResponse.ParsedQuestion> parseImportResponse(String response) {
        try {
            String json = com.interviewassistant.ai.util.JsonOutputUtils.extractJson(response);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.reader()
                    .with(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .readTree(json);
            com.fasterxml.jackson.databind.JsonNode items = root.path("items");
            if (!items.isArray()) {
                return java.util.Collections.emptyList();
            }
            java.util.List<ParseResponse.ParsedQuestion> result = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode item : items) {
                String question = textValue(item, "q", "question");
                if (question == null) continue;
                String answer = textValue(item, "a", "answer");
                result.add(new ParseResponse.ParsedQuestion(question, answer, listValue(item, "k", "keywords")));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("网页面试题解析失败: " + e.getMessage(), e);
        }
    }

    public String buildDeepDivePrompt(String question, List<String> expectedKeywords,
                                       DeepDiveContextType contextType, String contextContent,
                                       List<ChatMessage> messages) {
        List<String> keywords = expectedKeywords != null ? expectedKeywords : List.of();
        List<ChatMessage> compactMessages = compactDeepDiveMessages(messages);
        String history = compactMessages.stream()
                .map(m -> (m.getRole() == ChatRole.USER ? "候选人" : "教练") + "：" + compactText(m.getContent(), MAX_DEEP_DIVE_MESSAGE_CHARS))
                .collect(Collectors.joining("\n\n"));

        return promptService.render("interview/deep-dive.md", Map.ofEntries(
                Map.entry("question", question),
                Map.entry("expectedKeywords", keywords),
                Map.entry("contextType", contextType == DeepDiveContextType.RECOMMENDED_ANSWER ? "推荐答案" : "反馈点评"),
                Map.entry("contextContent", compactDeepDiveContext(contextContent, keywords, latestUserQuestion(compactMessages))),
                Map.entry("history", history),
                Map.entry("styleInstruction", "")
        ));
    }

    private List<ChatMessage> compactDeepDiveMessages(List<ChatMessage> messages) {
        if (messages == null || messages.size() <= MAX_DEEP_DIVE_MESSAGES) {
            return messages != null ? messages : List.of();
        }
        return messages.subList(messages.size() - MAX_DEEP_DIVE_MESSAGES, messages.size());
    }

    private String compactDeepDiveContext(String content, List<String> expectedKeywords, String latestUserQuestion) {
        String normalized = normalizeDeepDiveText(content);
        if (normalized.length() <= MAX_DEEP_DIVE_CONTEXT_CHARS) {
            return normalized;
        }

        List<String> segments = splitDeepDiveSegments(normalized);
        Set<String> questionTerms = extractQuestionTerms(latestUserQuestion);
        List<DeepDiveSegment> ranked = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            int score = scoreDeepDiveSegment(segments.get(i), expectedKeywords, questionTerms);
            if (score > 0) {
                ranked.add(new DeepDiveSegment(i, segments.get(i), score));
            }
        }
        ranked.sort(Comparator
                .comparingInt(DeepDiveSegment::score)
                .reversed()
                .thenComparingInt(DeepDiveSegment::index));

        Set<Integer> selectedIndexes = new TreeSet<>();
        int selectedChars = 0;
        for (DeepDiveSegment segment : ranked) {
            for (int index = Math.max(0, segment.index() - 1);
                 index <= Math.min(segments.size() - 1, segment.index() + 1);
                 index++) {
                if (selectedIndexes.contains(index)) {
                    continue;
                }
                int nextLength = segments.get(index).length() + 3;
                if (selectedChars + nextLength > DEEP_DIVE_EXCERPT_CHARS) {
                    continue;
                }
                selectedIndexes.add(index);
                selectedChars += nextLength;
            }
        }
        if (selectedIndexes.isEmpty()) {
            return buildFallbackCompactContext(normalized);
        }

        StringBuilder result = new StringBuilder("【上下文压缩说明】以下为原文提取式摘录，未经过 AI 改写；保留事实、技术术语、因果关系、对比场景和候选人困惑，已删去客套话、重复段落和纯格式内容。\n\n");
        result.append("【高信号摘录】\n");
        for (Integer index : selectedIndexes) {
            appendWithinBudget(result, "- " + segments.get(index), DEEP_DIVE_EXCERPT_CHARS + 300);
        }
        String background = compactText(normalized, DEEP_DIVE_OPENING_CHARS);
        int remainingForBackground = MAX_DEEP_DIVE_CONTEXT_CHARS - result.length() - "\n【必要背景】\n".length();
        if (remainingForBackground > 100 && !background.isBlank()) {
            result.append("\n【必要背景】\n");
            appendWithinBudget(result, background, MAX_DEEP_DIVE_CONTEXT_CHARS);
        } else {
            result.append("\n...（因篇幅限制，省略必要背景段）");
        }
        return compactText(result.toString(), MAX_DEEP_DIVE_CONTEXT_CHARS);
    }

    private String buildFallbackCompactContext(String normalized) {
        int halfBudget = MAX_DEEP_DIVE_CONTEXT_CHARS / 2;
        String header = "【上下文压缩说明】原文未检测到高信号段落，以下为原文首尾摘要。\n\n";
        String head = normalized.substring(0, Math.min(halfBudget, normalized.length()));
        if (normalized.length() > halfBudget) {
            String tail = normalized.substring(normalized.length() - halfBudget);
            return header + "【原文前半】\n" + head + "\n\n...（省略）\n\n【原文后半】\n" + tail;
        }
        return header + head;
    }

    private String latestUserQuestion(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getRole() == ChatRole.USER) {
                return message.getContent();
            }
        }
        return "";
    }

    private List<String> splitDeepDiveSegments(String text) {
        List<String> segments = new ArrayList<>();
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.matches("^[-*_\\s]{3,}$")) {
                continue;
            }
            if (line.startsWith("#")) {
                segments.add(line);
                continue;
            }
            StringBuilder sentence = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                sentence.append(ch);
                if ("。！？!?；;".indexOf(ch) >= 0 || sentence.length() >= 240) {
                    addDeepDiveSegment(segments, sentence.toString());
                    sentence.setLength(0);
                }
            }
            addDeepDiveSegment(segments, sentence.toString());
        }
        return segments;
    }

    private void addDeepDiveSegment(List<String> segments, String text) {
        String segment = text.trim();
        if (!segment.isBlank() && !segment.matches("^[-*_\\s]{3,}$")) {
            segments.add(segment);
        }
    }

    private int scoreDeepDiveSegment(String text, List<String> expectedKeywords, Set<String> questionTerms) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        int score = 0;
        long keywordHits = expectedKeywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .filter(keyword -> lowerText.contains(keyword.toLowerCase(Locale.ROOT)))
                .limit(2)
                .count();
        score += keywordHits * 5;

        long questionTermHits = questionTerms.stream()
                .filter(term -> lowerText.contains(term.toLowerCase(Locale.ROOT)))
                .limit(2)
                .count();
        score += questionTermHits * 5;

        if (text.matches(".*(因为|所以|因此|导致|原因|本质|取决于|意味着).*")) {
            score += 4;
        }
        if (text.matches(".*(区别|相比|而不是|优点|缺点|优势|劣势|不同).*")) {
            score += 3;
        }
        if (text.matches(".*(适合|场景|例如|比如|实践中|生产环境|实际项目).*")) {
            score += 3;
        }
        if (text.matches(".*(风险|问题|误区|遗漏|注意|瓶颈|限制|未命中|命中).*")) {
            score += 3;
        }
        if (text.startsWith("#")) {
            score += 2;
        }
        if (text.contains("？") || text.contains("?")) {
            score += 3;
        }
        if (looksTechnical(text)) {
            score += 2;
        }
        if (isLowValueDeepDiveSegment(text)) {
            score -= 4;
        }
        return score;
    }

    private Set<String> extractQuestionTerms(String question) {
        String normalized = normalizeDeepDiveText(question);
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        Arrays.stream(normalized.split("[\\s,，。！？?；;：:、()（）\\[\\]{}<>《》\"'`]+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .forEach(terms::add);

        String hanOnly = normalized.replaceAll("[^\\p{IsHan}A-Za-z0-9+#._-]", "");
        for (int start = 0; start < hanOnly.length(); start++) {
            for (int len = 2; len <= 4 && start + len <= hanOnly.length(); len++) {
                String term = hanOnly.substring(start, start + len);
                if (!isQuestionStopTerm(term)) {
                    terms.add(term);
                }
            }
            if (terms.size() >= 120) {
                break;
            }
        }
        return terms;
    }

    private boolean isQuestionStopTerm(String term) {
        return Set.of("为什么", "怎么", "如何", "是否", "是不是", "这个", "那个", "一下", "哪些", "什么", "区别", "影响").contains(term);
    }

    private boolean looksTechnical(String text) {
        return text.matches(".*[A-Za-z0-9][A-Za-z0-9+#._/-]{1,}.*")
                || text.matches(".*(索引|缓存|线程|事务|锁|队列|内存|磁盘|网络|接口|模型|节点|算法|复杂度|数据库|服务|架构).*");
    }

    private boolean isLowValueDeepDiveSegment(String text) {
        return text.matches(".*(继续加油|整体不错|表达清晰|可以更好|建议你|下面我们|总的来说|希望你|简单来说).*")
                && !looksTechnical(text);
    }

    private void appendWithinBudget(StringBuilder builder, String text, int budget) {
        if (builder.length() >= budget || text == null || text.isBlank()) {
            return;
        }
        int remaining = budget - builder.length();
        if (text.length() <= remaining) {
            builder.append(text).append('\n');
        } else if (remaining > 80) {
            builder.append(text, 0, remaining - 20).append("...\n");
        }
    }

    private String compactText(String text, int maxChars) {
        String normalized = normalizeDeepDiveText(text);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxChars - 20)).stripTrailing() + "\n...（已压缩）";
    }

    private String normalizeDeepDiveText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    public List<BatchQuestionItem> generateBatchQuestions(InterviewDirection direction, InterviewLevel level, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<BatchQuestionItem> allQuestions = new ArrayList<>();
        int batches = (count + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int batch = 0; batch < batches; batch++) {
            int remaining = count - allQuestions.size();
            if (remaining <= 0) {
                break;
            }
            int batchCount = Math.min(remaining, BATCH_SIZE);

            String userMessage = promptService.render("interview/batch-question.md", Map.ofEntries(
                    Map.entry("direction", InterviewLabels.directionLabel(direction)),
                    Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                    Map.entry("count", String.valueOf(batchCount)),
                    Map.entry("batchNumber", String.valueOf(batch + 1)),
                    Map.entry("startIndex", String.valueOf(allQuestions.size() + 1)),
                    Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
            ));

            List<BatchQuestionItem> batchResult = generateBatchWithRetry(userMessage, batch + 1);
            if (batchResult.isEmpty()) {
                log.warn("Batch {} returned null, skipping", batch + 1);
                continue;
            }

            int startId = allQuestions.size() + 1;
            for (int i = 0; i < batchResult.size(); i++) {
                BatchQuestionItem item = batchResult.get(i);
                item.setQuestionId(String.format("batch_%03d", startId + i));
                allQuestions.add(item);
                if (allQuestions.size() >= count) {
                    break;
                }
            }
        }

        return allQuestions;
    }

    public List<BatchQuestionItem> generateBatchQuestionChunk(InterviewDirection direction, InterviewLevel level,
                                                              int count, int batchNumber,
                                                              int startIndex, List<String> existingQuestions) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        String existingText = formatExistingQuestions(existingQuestions);

        String userMessage = promptService.render("interview/batch-question-only.md", Map.ofEntries(
                Map.entry("direction", InterviewLabels.directionLabel(direction)),
                Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
                Map.entry("count", String.valueOf(count)),
                Map.entry("batchNumber", String.valueOf(batchNumber)),
                Map.entry("startIndex", String.valueOf(startIndex)),
                Map.entry("existingQuestions", existingText),
                Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
        ));

        List<BatchQuestionItem> batchResult = generateBatchWithRetry(userMessage, batchNumber);
        if (batchResult.isEmpty()) {
            return Collections.emptyList();
        }

        List<BatchQuestionItem> questions = new ArrayList<>();
        for (int i = 0; i < batchResult.size() && questions.size() < count; i++) {
            BatchQuestionItem item = batchResult.get(i);
            item.setQuestionId(String.format("batch_%03d", startIndex + i));
            questions.add(item);
        }
        return questions;
    }

    public int batchSize() {
        return BATCH_SIZE;
    }

    private String formatExistingQuestions(List<String> existingQuestions) {
        if (existingQuestions == null || existingQuestions.isEmpty()) {
            return "（无已有题目，这是第一批）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < existingQuestions.size(); i++) {
            sb.append(i + 1).append(". ").append(existingQuestions.get(i)).append('\n');
        }
        String text = sb.toString();
        if (text.length() > 3000) {
            return text.substring(0, 3000) + "\n...（共有 " + existingQuestions.size() + " 道已有题目，已截断）";
        }
        return text;
    }

    private List<BatchQuestionItem> generateBatchWithRetry(String userMessage, int batchNumber) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_BATCH_ATTEMPTS; attempt++) {
            long startedAt = System.currentTimeMillis();
            try {
                log.info("Batch {} attempt {}/{} started", batchNumber, attempt, MAX_BATCH_ATTEMPTS);
                String response = aiGateway.generateText(promptService.load("interview/system.md"), userMessage);
                List<BatchQuestionItem> items = parseCompactBatch(response);
                log.info("Batch {} attempt {}/{} finished in {} ms, parsed {} question(s)",
                        batchNumber, attempt, MAX_BATCH_ATTEMPTS,
                        System.currentTimeMillis() - startedAt, items.size());
                return items;
            } catch (RuntimeException e) {
                lastError = e;
                if (AiErrorUtils.isRateLimit(e) || AiErrorUtils.isUnauthorized(e)) {
                    log.warn("Batch {} failed without retry: {}", batchNumber, AiErrorUtils.compactMessage(e));
                    throw e;
                }
                log.warn("Batch {} attempt {}/{} failed in {} ms: {}",
                        batchNumber, attempt, MAX_BATCH_ATTEMPTS,
                        System.currentTimeMillis() - startedAt, e.getMessage());
                if (attempt < MAX_BATCH_ATTEMPTS) {
                    sleepBeforeRetry(batchNumber, attempt, e);
                }
            }
        }
        throw lastError;
    }

    private void sleepBeforeRetry(int batchNumber, int attempt, RuntimeException error) {
        long delayMs = INITIAL_BATCH_RETRY_DELAY_MS * attempt;
        if (AiErrorUtils.isNetworkError(error)) {
            delayMs *= 2;
        }
        log.info("Batch {} will retry after {} ms", batchNumber, delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("批量出题重试已中断", interrupted);
        }
    }

    private List<BatchQuestionItem> parseCompactBatch(String response) {
        try {
            String json = JsonOutputUtils.extractJson(response);
            JsonNode root = objectMapper.reader()
                    .with(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .readTree(json);
            JsonNode items = root.path("items");
            if (items.isMissingNode()) {
                items = root.path("questions");
            }
            if (items.isMissingNode() && root.isArray()) {
                items = root;
            }
            if (!items.isArray()) {
                return Collections.emptyList();
            }

            List<BatchQuestionItem> result = new ArrayList<>();
            for (JsonNode item : items) {
                String question = textValue(item, "q", "question");
                if (question == null) {
                    continue;
                }
                String answer = textValue(item, "a", "answer", "reference_answer");
                result.add(new BatchQuestionItem(null, question, answer, listValue(item, "k", "keywords")));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("批量题紧凑 JSON 解析失败，响应预览: " + preview(response), e);
        }
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private List<String> listValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isArray()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : value) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            }
            return values;
        }
        return Collections.emptyList();
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
        return normalized.length() > 500 ? normalized.substring(0, 500) + "..." : normalized;
    }
}
