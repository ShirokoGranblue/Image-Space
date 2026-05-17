package com.picmgmt.image;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImageUpdateDTO {
    @Size(max = 255, message = "图片名称不能超过255个字符")
    private String imageName;
    private Long categoryId;
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    private String tags;
    @Pattern(regexp = "(?i)PUBLIC|PRIVATE|SPECIFIED", message = "可见权限无效")
    private String visibility;
    private String visibleUsernames;
}
