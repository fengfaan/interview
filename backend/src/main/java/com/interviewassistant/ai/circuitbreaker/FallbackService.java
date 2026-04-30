package com.interviewassistant.ai.circuitbreaker;

import com.interviewassistant.ai.config.AiConfig;
import com.interviewassistant.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackService {

    private final ModelCircuitBreaker circuitBreaker;
    private final SettingsService settingsService;

    /**
     * Find a healthy alternative model and switch to it.
     * Priority: other OpenRouter free models -> ZhiPu glm-4-flash.
     *
     * @return the model name switched to, or null if all models are down
     */
    public String fallback(String currentModel) {
        String currentProvider = settingsService.getCurrentProvider();

        if (AiConfig.PROVIDER_OPENROUTER.equals(currentProvider)) {
            String candidate = findHealthyOpenRouterModel(currentModel);
            if (candidate != null) {
                switchModel(AiConfig.PROVIDER_OPENROUTER, candidate);
                return candidate;
            }
            // All OpenRouter models OPEN -> fall back to ZhiPu
            log.warn("All OpenRouter free models OPEN, falling back to ZhiPu {}", AiConfig.DEFAULT_MODEL);
            if (!circuitBreaker.isOpen(AiConfig.DEFAULT_MODEL)) {
                switchModel(AiConfig.PROVIDER_ZHIPU, AiConfig.DEFAULT_MODEL);
                return AiConfig.DEFAULT_MODEL;
            }
        } else {
            // ZhiPu model failed -> try OpenRouter free models
            String candidate = findHealthyOpenRouterModel(null);
            if (candidate != null) {
                switchModel(AiConfig.PROVIDER_OPENROUTER, candidate);
                return candidate;
            }
        }

        log.error("All models are OPEN, no fallback available");
        return null;
    }

    private String findHealthyOpenRouterModel(String excludeModel) {
        List<String> models = settingsService.getModelOptions(AiConfig.PROVIDER_OPENROUTER);
        for (String model : models) {
            if (model.equals(excludeModel)) {
                continue;
            }
            if (!circuitBreaker.isOpen(model)) {
                return model;
            }
        }
        return null;
    }

    private void switchModel(String provider, String model) {
        log.warn("Circuit breaker triggered, switching model: provider={}, model={}", provider, model);
        settingsService.saveModel(provider, model);
    }
}
