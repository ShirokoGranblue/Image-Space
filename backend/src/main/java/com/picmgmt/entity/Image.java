package com.picmgmt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("images")
public class Image {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private Long userId;
    private Long categoryId;
    private String imageName;
    private String imagePath;
    private String storageKey;
    private Long fileSize;
    private String imageType;
    private String description;
    private String tags;
    private String visibility;
    private Long mediaVersion;
    private String visibleUsernames;
    private LocalDateTime uploadTime;
}
