package com.interviewassistant.dto.resume;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RewriteStreamRequest {
    @NotBlank(message = "jobDescription is required")
    @Size(max = 20000, message = "jobDescription must not exceed 20000 characters")
    private String jobDescription;

    @NotBlank(message = "resume is required")
    @Size(max = 20000, message = "resume must not exceed 20000 characters")
    private String resume;

    @Valid
    private SuggestionInput suggestion;

    @Data
    public static class SuggestionInput {
        @NotBlank
        private String id;
        private String title;
        @NotBlank
        private String sourceText;
    }
}
