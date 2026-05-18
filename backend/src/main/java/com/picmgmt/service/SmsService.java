package com.picmgmt.service;

public interface SmsService {

    void sendVerificationCode(String phone, String code);
}
