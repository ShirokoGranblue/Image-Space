package com.picmgmt.service.impl;

final class EmailAddressMasker {

    private EmailAddressMasker() {}

    static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "***";
        }
        String localPart = email.substring(0, atIndex);
        String maskedLocal = localPart.length() == 1
                ? localPart + "***"
                : localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        return maskedLocal + email.substring(atIndex);
    }
}
