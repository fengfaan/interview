package com.interviewassistant.dto.resume;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PolishStreamRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldRejectBlankSourceText() {
        PolishStreamRequest request = new PolishStreamRequest();
        request.setSourceText("");
        
        Set<ConstraintViolation<PolishStreamRequest>> violations = validator.validate(request);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("待改写内容不能为空")));
    }
}
