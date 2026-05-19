package com.picmgmt.service;

public interface SmsService {

    void sendVerificationCode(String phone);

    boolean checkVerificationCode(String phone, String code);
}
