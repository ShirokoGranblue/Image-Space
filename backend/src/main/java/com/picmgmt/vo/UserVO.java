package com.picmgmt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String uuid;
    private String username;
    private String displayName;
    private String role;
    private String avatar;
    private String avatarUrl;
    private String email;
    private String phone;
    private String bio;
    private String background;
    private String backgroundUrl;
    private LocalDateTime createTime;
}
