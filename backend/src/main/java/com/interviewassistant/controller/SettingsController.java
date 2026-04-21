package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.settings.ApiKeyRequest;
import com.interviewassistant.dto.settings.ApiKeyResponse;
import com.interviewassistant.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/apikey")
    public ApiResponse<ApiKeyResponse> getApiKey() {
        String key = settingsService.getCurrentApiKey();
        boolean configured = key != null && !key.isBlank();
        String masked = configured ? settingsService.maskKey(key) : "";
        return ApiResponse.ok(new ApiKeyResponse(masked, configured));
    }

    @PostMapping("/apikey")
    public ApiResponse<Void> saveApiKey(@Valid @RequestBody ApiKeyRequest request) {
        settingsService.saveApiKey(request.getApiKey());
        return ApiResponse.ok(null);
    }
}
