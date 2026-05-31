package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageReadServiceTest {

    @Mock private ImageRepository imageRepository;
    @Mock private ImageMapper imageMapper;
    @Mock private ImageLikeMapper imageLikeMapper;
    @Mock private ImagePermissionService permissionService;
    @Mock private StorageService storageService;

    private ImageReadService service;

    @BeforeEach
    void setUp() {
        service = new ImageReadService(imageRepository, imageMapper, imageLikeMapper, permissionService, storageService);
    }

    @Test
    void getSquare_shouldDefaultToStableRandomOrder() {
        when(imageMapper.selectImageVOPage(any(), isNull(), eq("summer.jpg"), isNull(), eq("PUBLIC"),
                any(), eq("upload_time"), eq("desc"), eq("random"), eq("square")))
                .thenReturn(new Page<ImageVO>(1, 12));

        service.getSquare(1, 12, "summer.jpg", "landscape", null, null, null, null);

        verify(imageMapper).selectImageVOPage(any(), isNull(), eq("summer.jpg"), isNull(), eq("PUBLIC"),
                any(), eq("upload_time"), eq("desc"), eq("random"), eq("square"));
    }

    @Test
    void getSquare_shouldPassSafeTimeOrSizeSortWhenRandomIsDisabled() {
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("file_size"), eq("asc"), eq("latest"), isNull()))
                .thenReturn(new Page<ImageVO>(1, 12));

        service.getSquare(1, 12, null, null, "latest", null, "file_size", "asc");

        verify(imageMapper).selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("file_size"), eq("asc"), eq("latest"), isNull());
    }

    @Test
    void getSquare_shouldAllowNameSortAscendingForNumericThenAlphabeticOrder() {
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("image_name"), eq("asc"), eq("latest"), isNull()))
                .thenReturn(new Page<ImageVO>(1, 12));

        service.getSquare(1, 12, null, null, "latest", null, "image_name", "asc");

        verify(imageMapper).selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("image_name"), eq("asc"), eq("latest"), isNull());
    }

    @Test
    void getSquare_shouldParseHashSeparatedTagsAndKeepCommasInsideTag() {
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                any(), eq("upload_time"), eq("desc"), eq("random"), eq("seed-1")))
                .thenReturn(new Page<ImageVO>(1, 12));

        service.getSquare(1, 12, null, "cute,blue#avatar", "random", "seed-1", null, null);

        ArgumentCaptor<List<String>> tagFilters = ArgumentCaptor.forClass(List.class);
        verify(imageMapper).selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                tagFilters.capture(), eq("upload_time"), eq("desc"), eq("random"), eq("seed-1"));
        Assertions.assertEquals(List.of("cute,blue", "avatar"), tagFilters.getValue());
    }

    @Test
    void getSquare_shouldReturnVersionedBackendDownloadUrlForPublicStorageImages() {
        Page<ImageVO> page = new Page<>(1, 30);
        ImageVO vo = new ImageVO();
        vo.setId(7L);
        vo.setUuid("img-public-uuid");
        vo.setVisibility("PUBLIC");
        vo.setStorageKey("4/summer.png");
        page.setRecords(List.of(vo));
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("random"), eq("square")))
                .thenReturn(page);

        Page<ImageVO> result;
        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(false);
            result = service.getSquare(1, 30, null, null, null, null, null, null);
        }

        assertEquals("/api/image/download/img-public-uuid?v=3864dd6014f3", result.getRecords().get(0).getImageUrl());
        verify(storageService, never()).getPresignedUrl(any(), any(), any());
    }
}
