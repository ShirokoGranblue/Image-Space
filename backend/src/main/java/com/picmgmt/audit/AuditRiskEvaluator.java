package com.picmgmt.audit;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AuditRiskEvaluator {

    public String evaluate(String action, String module, String status, String path) {
        String normalizedAction = normalize(action);
        String normalizedModule = normalize(module);
        String normalizedStatus = normalize(status);
        String normalizedPath = normalize(path);

        if ("FAILED".equals(normalizedStatus) || "FAIL".equals(normalizedStatus)
                || normalizedAction.contains("DELETE")
                || normalizedAction.contains("PASSWORD")
                || normalizedAction.contains("ACCOUNT")
                || normalizedAction.contains("SECURITY")
                || normalizedPath.contains("/DELETE")) {
            return "HIGH";
        }
        if (normalizedAction.contains("LOGIN")
                || normalizedAction.contains("UPLOAD")
                || normalizedAction.contains("UPDATE")
                || normalizedAction.contains("ADD")
                || normalizedAction.contains("CREATE")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
