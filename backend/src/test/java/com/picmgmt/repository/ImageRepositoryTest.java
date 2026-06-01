package com.picmgmt.repository;

import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Image;
import com.picmgmt.image.ImageUrlService;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ImageRepositoryTest {

    @Mock private ImageMapper imageMapper;
    @Mock private UserMapper userMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private StorageService storageService;
    @Mock private CacheService cacheService;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private ImageUrlService imageUrlService;

    @Test
    void toVO_shouldUseDirectCdnUrlForPublicStorageImages() {
        ImageRepository repository = new ImageRepository(
                imageMapper, userMapper, categoryMapper, storageService, cacheService, bloomFilterService, imageUrlService);
        Image image = new Image();
        image.setId(7L);
        image.setUuid("img-public-uuid");
        image.setUserId(4L);
        image.setVisibility("PUBLIC");
        image.setStorageKey("4/summer.png");
        image.setUploadTime(LocalDateTime.of(2026, 6, 1, 12, 0, 0));

        String cdnUrl = "https://cdn.image-space.app/4/summer.png?v=6813f5a73c3c";
        when(imageUrlService.getPublicImageUrl(eq("4/summer.png"), any(LocalDateTime.class))).thenReturn(cdnUrl);

        ImageVO vo = repository.toVO(image);

        assertEquals(cdnUrl, vo.getImageUrl());
        verify(imageUrlService).getPublicImageUrl(eq("4/summer.png"), any(LocalDateTime.class));
        verify(imageUrlService, never()).getPrivateImageUrl(any());
    }

    @Test
    void toVO_shouldUseBackendAuthorizedUrlForPrivateStorageImages() {
        ImageRepository repository = new ImageRepository(
                imageMapper, userMapper, categoryMapper, storageService, cacheService, bloomFilterService, imageUrlService);
        Image image = new Image();
        image.setId(7L);
        image.setUuid("img-private-uuid");
        image.setUserId(4L);
        image.setVisibility("PRIVATE");
        image.setStorageKey("4/summer.png");

        String privateUrl = "https://cdn.image-space.app/4/summer.png?auth=abc&expires=1893456000";
        when(imageUrlService.getPrivateImageUrl("4/summer.png")).thenReturn(privateUrl);

        ImageVO vo = repository.toVO(image);

        assertEquals(privateUrl, vo.getImageUrl());
        verify(imageUrlService).getPrivateImageUrl("4/summer.png");
    }
}
