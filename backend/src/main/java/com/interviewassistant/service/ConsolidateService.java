package com.interviewassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.dto.import_.ConsolidatedCategory;
import com.interviewassistant.dto.import_.ConsolidateResult;
import com.interviewassistant.dto.import_.ParseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsolidateService {

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    public ConsolidateResult consolidate(List<ParseResponse.ParsedQuestion> items) {
        String questionsJson = serializeQuestions(items);

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("questions", questionsJson);

        String userMessage = promptService.render("import/consolidate.md", variables);
        String systemPrompt = promptService.load("interview/system.md");

        String response = aiGateway.generateText(systemPrompt, userMessage);

        return parseConsolidateResponse(response, items.size());
    }

    private String serializeQuestions(List<ParseResponse.ParsedQuestion> items) {
        try {
            List<Map<String, Object>> serialized = items.stream().map(item -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("q", item.getQuestion());
                map.put("a", item.getAnswer() != null ? item.getAnswer() : "");
                map.put("k", item.getKeywords() != null ? item.getKeywords() : List.of());
                return map;
            }).toList();
            return objectMapper.writeValueAsString(serialized);
        } catch (Exception e) {
            throw new RuntimeException("序列化题目失败", e);
        }
    }

    ConsolidateResult parseConsolidateResponse(String response, int originalCount) {
        try {
            String json = com.interviewassistant.ai.util.JsonOutputUtils.extractJson(response);
            JsonNode root = objectMapper.readTree(json);

            ConsolidateResult result = new ConsolidateResult();
            List<ConsolidatedCategory> categories = new ArrayList<>();

            JsonNode categoriesNode = root.get("categories");
            if (categoriesNode != null && categoriesNode.isArray()) {
                for (JsonNode catNode : categoriesNode) {
                    ConsolidatedCategory category = new ConsolidatedCategory();
                    category.setName(catNode.path("name").asText("综合"));

                    List<ParseResponse.ParsedQuestion> catItems = new ArrayList<>();
                    JsonNode itemsNode = catNode.get("items");
                    if (itemsNode != null && itemsNode.isArray()) {
                        for (JsonNode itemNode : itemsNode) {
                            ParseResponse.ParsedQuestion pq = new ParseResponse.ParsedQuestion();
                            pq.setQuestion(textValue(itemNode, "q", "question"));
                            pq.setAnswer(textValue(itemNode, "a", "answer"));
                            pq.setKeywords(listValue(itemNode, "k", "keywords"));
                            catItems.add(pq);
                        }
                    }
                    category.setItems(catItems);
                    categories.add(category);
                }
            }

            result.setCategories(categories);
            result.setDedupCount(root.path("dedupCount").asInt(0));

            int totalItems = categories.stream()
                    .mapToInt(c -> c.getItems().size())
                    .sum();
            result.setTotalCount(totalItems);

            if (result.getDedupCount() == 0 && totalItems < originalCount) {
                result.setDedupCount(originalCount - totalItems);
            }

            return result;
        } catch (Exception e) {
            log.error("解析清洗结果失败: {}", e.getMessage());
            throw new RuntimeException("解析清洗结果失败: " + e.getMessage(), e);
        }
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode field = node.get(name);
            if (field != null && !field.isNull() && !field.asText().isEmpty()) {
                return field.asText();
            }
        }
        return "";
    }

    private List<String> listValue(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode field = node.get(name);
            if (field != null && field.isArray()) {
                List<String> result = new ArrayList<>();
                for (JsonNode item : field) {
                    result.add(item.asText());
                }
                return result;
            }
        }
        return new ArrayList<>();
    }
}
