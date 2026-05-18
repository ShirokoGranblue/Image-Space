package com.picmgmt.service;

public interface EmailService {

    void sendVerificationCode(String to, String code);
}
