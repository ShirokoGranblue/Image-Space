package com.picmgmt.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void finalAuthenticationRequestsRequireGraphicalCaptcha() {
        assertCaptchaRequired(new RegisterDTO());
        assertCaptchaRequired(new LoginDTO());
        assertCaptchaRequired(new CodeLoginDTO());
    }

    @Test
    void sendCodeRequestDoesNotRequireGraphicalCaptcha() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setEmail("member@example.com");

        Set<String> invalidFields = invalidFields(dto);

        assertFalse(invalidFields.contains("captchaId"));
        assertFalse(invalidFields.contains("captchaCode"));
    }

    private void assertCaptchaRequired(Object dto) {
        Set<String> invalidFields = invalidFields(dto);
        assertTrue(invalidFields.contains("captchaId"));
        assertTrue(invalidFields.contains("captchaCode"));
    }

    private Set<String> invalidFields(Object dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
