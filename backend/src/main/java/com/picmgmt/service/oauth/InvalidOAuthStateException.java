package com.picmgmt.service.oauth;

public class InvalidOAuthStateException extends RuntimeException {

    public InvalidOAuthStateException(String provider) {
        super(provider + " OAuth state is invalid or expired");
    }
}
