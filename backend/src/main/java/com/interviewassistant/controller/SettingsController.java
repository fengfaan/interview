package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.ai.config.AiConfig;
import com.interviewassistant.dto.settings.ApiKeyRequest;
import com.interviewassistant.dto.settings.ApiKeyResponse;
import com.interviewassistant.dto.settings.ModelRequest;
import com.interviewassistant.dto.settings.ModelResponse;
import com.interviewassistant.dto.settings.PromptContentResponse;
import com.interviewassistant.dto.settings.PromptFileResponse;
import com.interviewassistant.dto.settings.PromptImproveRequest;
import com.interviewassistant.dto.settings.PromptImproveResponse;
import com.interviewassistant.dto.knowledge.VaultConfigRequest;
import com.interviewassistant.dto.knowledge.VaultConfigResponse;
import com.interviewassistant.dto.settings.PromptSaveRequest;
import com.interviewassistant.dto.settings.ModelHealthEntry;
import com.interviewassistant.ai.circuitbreaker.ModelCircuitBreaker;
import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final PromptService promptService;
    private final AiGateway aiGateway;
    private final ModelCircuitBreaker circuitBreaker;

    @GetMapping("/apikey")
    public ApiResponse<ApiKeyResponse> getApiKey(@RequestParam(value = "provider", required = false) String provider) {
        String key = provider == null || provider.isBlank()
                ? settingsService.getCurrentApiKey()
                : settingsService.getApiKeyForProvider(provider);
        boolean configured = key != null && !key.isBlank();
        String masked = configured ? settingsService.maskKey(key) : "";
        return ApiResponse.ok(new ApiKeyResponse(masked, configured));
    }

    @PostMapping("/apikey")
    public ApiResponse<Void> saveApiKey(@Valid @RequestBody ApiKeyRequest request) {
        settingsService.saveApiKey(request.getProvider(), request.getApiKey());
        return ApiResponse.ok(null);
    }

    @GetMapping("/model")
    public ApiResponse<ModelResponse> getModel(@RequestParam(value = "provider", required = false) String provider) {
        String responseProvider = provider == null || provider.isBlank()
                ? settingsService.getCurrentProvider()
                : AiConfig.normalizeProvider(provider);
        return ApiResponse.ok(new ModelResponse(
                responseProvider,
                settingsService.getModelForProvider(responseProvider),
                settingsService.getDefaultModel(responseProvider),
                settingsService.getModelOptions(responseProvider)
        ));
    }

    @PostMapping("/model")
    public ApiResponse<ModelResponse> saveModel(@Valid @RequestBody ModelRequest request) {
        settingsService.saveModel(request.getProvider(), request.getModel());
        return getModel(null);
    }

    @GetMapping("/model-health")
    public ApiResponse<List<ModelHealthEntry>> getModelHealth() {
        String provider = settingsService.getCurrentProvider();
        java.util.List<String> models = new java.util.ArrayList<>(settingsService.getModelOptions(provider));
        String otherProvider = AiConfig.PROVIDER_OPENROUTER.equals(provider)
                ? AiConfig.PROVIDER_ZHIPU : AiConfig.PROVIDER_OPENROUTER;
        models.addAll(settingsService.getModelOptions(otherProvider));
        List<ModelHealthEntry> health = circuitBreaker.getHealthForAll(models).stream()
                .map(e -> new ModelHealthEntry(e.model(), e.state().name(), e.failureCount(), e.openedAt()))
                .collect(java.util.stream.Collectors.toList());
        return ApiResponse.ok(health);
    }

    @GetMapping("/prompts")
    public ApiResponse<List<PromptFileResponse>> listPrompts() {
        return ApiResponse.ok(promptService.listFiles());
    }

    @GetMapping("/vault")
    public ApiResponse<VaultConfigResponse> getVaultConfig() {
        String path = settingsService.getVaultPath();
        boolean configured = settingsService.isVaultPathValid();
        return ApiResponse.ok(new VaultConfigResponse(configured, configured ? path : null));
    }

    @PostMapping("/vault")
    public ApiResponse<VaultConfigResponse> saveVaultConfig(@Valid @RequestBody VaultConfigRequest request) {
        try {
            settingsService.saveVaultPath(request.getPath());
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("INVALID_VAULT_PATH", e.getMessage());
        }
        String path = settingsService.getVaultPath();
        return ApiResponse.ok(new VaultConfigResponse(true, path));
    }

    @GetMapping("/prompts/content")
    public ApiResponse<PromptContentResponse> getPromptContent(@RequestParam("path") String path) {
        return ApiResponse.ok(new PromptContentResponse(path, promptService.load(path)));
    }

    @PutMapping("/prompts/content")
    public ApiResponse<PromptContentResponse> savePromptContent(@Valid @RequestBody PromptSaveRequest request) {
        promptService.save(request.getPath(), request.getContent());
        return ApiResponse.ok(new PromptContentResponse(request.getPath(), promptService.load(request.getPath())));
    }

    @PostMapping("/prompts/improve")
    public ApiResponse<PromptImproveResponse> improvePrompt(@Valid @RequestBody PromptImproveRequest request) {
        String instruction = request.getInstruction() == null || request.getInstruction().isBlank()
                ? "请在保持原有业务目标、变量占位符和输出格式约束的前提下，提高提示词的清晰度、稳定性和可控性。"
                : request.getInstruction();
        String prompt = promptService.render("settings/prompt-improver.md", Map.of(
                "path", request.getPath(),
                "instruction", instruction,
                "content", request.getContent()
        ));
        String improved = aiGateway.generateText(prompt);
        return ApiResponse.ok(new PromptImproveResponse(improved));
    }
}
