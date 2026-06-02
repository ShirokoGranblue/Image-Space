package com.picmgmt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaMetaVO {

    private Long imageId;
    private String storageKey;
    private String visibility;
    private Long ownerId;
    private Long version;
}
