package com.picmgmt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String avatar;
    private String email;
    private String phone;
    private String bio;
    private String background;
    private LocalDateTime createTime;
}
