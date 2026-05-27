package com.picmgmt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image_likes")
public class ImageLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long imageId;
    private Long userId;
    private LocalDateTime createTime;
}
