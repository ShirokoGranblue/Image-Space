package com.picmgmt.dto;

import lombok.Data;

@Data
public class ImageQueryDTO {

    private Integer page = 1;
    private Integer limit = 50;
    private String keyword;
    private Long categoryId;
    private String sortField = "upload_time";
    private String sortOrder = "desc";
    private Long targetUserId;
}
