package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    private String captchaId;

    private String captchaCode;

    private String purpose;

    private String turnstileToken;
}
