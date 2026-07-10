package com.picmgmt.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notLoginUsesHttp401AsWellAsBusinessCode() {
        Object response = handler.handleNotLogin(NotLoginException.newInstance(
                "satoken", "default-login", "未登录", null
        ));

        ResponseEntity<?> entity = assertInstanceOf(ResponseEntity.class, response);
        assertEquals(401, entity.getStatusCode().value());
        Result<?> body = assertInstanceOf(Result.class, entity.getBody());
        assertEquals(401, body.getCode());
    }

    @Test
    void imageNotFoundBusinessErrorUsesHttp404() {
        Object response = handler.handleBusiness(new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        ResponseEntity<?> entity = assertInstanceOf(ResponseEntity.class, response);
        assertEquals(404, entity.getStatusCode().value());
    }
}
