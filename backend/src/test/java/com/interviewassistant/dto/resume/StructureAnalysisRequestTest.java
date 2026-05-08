package com.interviewassistant.dto.resume;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureAnalysisRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldRejectBlankResume() {
        StructureAnalysisRequest request = new StructureAnalysisRequest();
        request.setResume("");
        
        Set<ConstraintViolation<StructureAnalysisRequest>> violations = validator.validate(request);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("简历内容不能为空")));
    }

    @Test
    void shouldRejectShortResume() {
        StructureAnalysisRequest request = new StructureAnalysisRequest();
        request.setResume("太短");
        
        Set<ConstraintViolation<StructureAnalysisRequest>> violations = validator.validate(request);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("简历内容过短，请提供完整的简历")));
    }
}
