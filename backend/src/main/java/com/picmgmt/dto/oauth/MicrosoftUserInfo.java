package com.picmgmt.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Microsoft Graph API /me 响应
 */
@Data
public class MicrosoftUserInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("mail")
    private String mail;

    @JsonProperty("userPrincipalName")
    private String userPrincipalName;

    /**
     * 获取显示邮箱，优先使用 mail，其次使用 userPrincipalName。
     * 用于展示目的。
     */
    public String getEmail() {
        if (mail != null && !mail.isBlank()) {
            return mail;
        }
        if (userPrincipalName != null && !userPrincipalName.isBlank()) {
            return userPrincipalName;
        }
        return null;
    }

    /**
     * 获取经过 Microsoft 验证的邮箱，用于账号绑定。
     *
     * 优先使用 userPrincipalName（UPN 是 Microsoft 的登录凭据，始终经过验证）。
     * 仅在 UPN 不像是标准邮箱格式时才 fallback 到 mail 字段。
     *
     * 安全说明：mail 字段可能是用户自行添加的别名或转发地址，
     * 不一定经过与 UPN 同等强度的验证，因此不应作为账号绑定的首选依据。
     */
    public String getVerifiedEmail() {
        // UPN 是 Microsoft 账号的登录凭据，始终经过验证
        if (userPrincipalName != null && !userPrincipalName.isBlank()
                && userPrincipalName.contains("@")) {
            return userPrincipalName;
        }
        // UPN 不是标准邮箱格式时（极少数组织账号），回退到 mail
        if (mail != null && !mail.isBlank() && mail.contains("@")) {
            return mail;
        }
        return null;
    }
}
