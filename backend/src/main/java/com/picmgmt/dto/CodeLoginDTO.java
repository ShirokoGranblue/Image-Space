package com.picmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodeLoginDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    private String turnstileToken;
}
