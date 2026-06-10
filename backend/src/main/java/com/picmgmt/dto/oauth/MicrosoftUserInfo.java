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
     * 获取邮箱，优先使用 mail，其次使用 userPrincipalName
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
}
