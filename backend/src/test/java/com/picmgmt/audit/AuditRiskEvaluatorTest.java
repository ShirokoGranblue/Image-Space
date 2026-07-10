package com.picmgmt.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditRiskEvaluatorTest {

    private final AuditRiskEvaluator evaluator = new AuditRiskEvaluator();

    @Test
    void evaluateMarksFailedOrDangerousActionsAsHigh() {
        assertEquals("HIGH", evaluator.evaluate("IMAGE_DELETE", "IMAGE", "SUCCESS", "/image/abc"));
        assertEquals("HIGH", evaluator.evaluate("IMAGE_UPDATE", "IMAGE", "FAIL", "/image/abc"));
        assertEquals("HIGH", evaluator.evaluate("USER_DELETE_ACCOUNT", "USER", "SUCCESS", "/user/account"));
        assertEquals("HIGH", evaluator.evaluate("USER_CHANGE_PASSWORD", "USER", "SUCCESS", "/user/password"));
    }

    @Test
    void evaluateMarksSensitiveButSuccessfulOperationsAsMedium() {
        assertEquals("MEDIUM", evaluator.evaluate("USER_LOGIN", "USER", "SUCCESS", "/user/login"));
        assertEquals("MEDIUM", evaluator.evaluate("IMAGE_UPLOAD", "IMAGE", "SUCCESS", "/image/upload"));
        assertEquals("MEDIUM", evaluator.evaluate("CATEGORY_UPDATE", "CATEGORY", "SUCCESS", "/category/1"));
    }

    @Test
    void evaluateDefaultsLowForRoutineSuccessfulOperations() {
        assertEquals("LOW", evaluator.evaluate("IMAGE_LIKE", "IMAGE", "SUCCESS", "/image/abc/like"));
        assertEquals("LOW", evaluator.evaluate("COMMENT_LIKE", "COMMENT", "SUCCESS", "/comment/1/like"));
    }
}
