package com.picmgmt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String username;
    private String displayName;
    private String password;
    private String role;
    private String avatar;
    private String avatarKey;
    private String email;
    private String phone;
    private String githubUsername;
    private String bio;
    private String background;
    private String backgroundKey;
    private Integer deleted;
    private LocalDateTime createTime;
}
