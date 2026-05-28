package com.picmgmt.repository;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ImageRepositoryTest {

    @Mock private ImageMapper imageMapper;
    @Mock private UserMapper userMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private StorageService storageService;
    @Mock private CacheService cacheService;

    @Test
    void toVO_shouldExposeVersionedDownloadUrlFromStorageKey() {
        ImageRepository repository = new ImageRepository(
                imageMapper, userMapper, categoryMapper, storageService, cacheService);
        Image image = new Image();
        image.setId(7L);
        image.setUserId(4L);
        image.setStorageKey("4/summer.png");

        ImageVO vo = repository.toVO(image);

        assertEquals("/api/image/download/7?v=3864dd6014f3", vo.getImageUrl());
        verifyNoInteractions(storageService);
    }
}
