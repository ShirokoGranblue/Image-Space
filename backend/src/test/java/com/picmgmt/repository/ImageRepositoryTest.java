package com.picmgmt.repository;

import com.picmgmt.cache.BloomFilterService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
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

    @Test
    void toVO_shouldUseVersionedBackendDownloadUrlForPublicStorageImages() {
        ImageRepository repository = new ImageRepository(
                imageMapper, userMapper, categoryMapper, storageService, cacheService, bloomFilterService);
        Image image = new Image();
        image.setId(7L);
        image.setUuid("img-public-uuid");
        image.setUserId(4L);
        image.setVisibility("PUBLIC");
        image.setStorageKey("4/summer.png");

        ImageVO vo = repository.toVO(image);

        assertEquals("/api/image/download/img-public-uuid?v=3864dd6014f3", vo.getImageUrl());
        verify(storageService, never()).getPresignedUrl(eq("images"), eq("4/summer.png"), any(Duration.class));
    }

    @Test
    void toVO_shouldUsePresignedUrlFromStorageServiceForPrivateStorageImages() {
        ImageRepository repository = new ImageRepository(
                imageMapper, userMapper, categoryMapper, storageService, cacheService, bloomFilterService);
        Image image = new Image();
        image.setId(7L);
        image.setUuid("img-private-uuid");
        image.setUserId(4L);
        image.setVisibility("PRIVATE");
        image.setStorageKey("4/summer.png");

        String presignedUrl = "https://cdn.image-space.app/images/4/summer.png?X-Amz-Expires=300&signature=abc";
        when(storageService.getPresignedUrl(eq("images"), eq("4/summer.png"), any(Duration.class)))
                .thenReturn(presignedUrl);

        ImageVO vo = repository.toVO(image);

        assertEquals(presignedUrl, vo.getImageUrl());
        verify(storageService).getPresignedUrl(eq("images"), eq("4/summer.png"), any(Duration.class));
    }
}
