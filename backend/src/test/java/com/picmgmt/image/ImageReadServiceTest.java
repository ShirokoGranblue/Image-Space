package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @Mock private UserMapper userMapper;
    @Mock private ImagePermissionService permissionService;
    @Mock private StorageService storageService;
    @Mock private ImageUrlService imageUrlService;

    private ImageReadService service;

    @BeforeEach
    void setUp() {
        service = new ImageReadService(imageRepository, imageMapper, imageLikeMapper, userMapper, permissionService, storageService, imageUrlService);
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

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> tagFilters = ArgumentCaptor.forClass(List.class);
        verify(imageMapper).selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                tagFilters.capture(), eq("upload_time"), eq("desc"), eq("random"), eq("seed-1"));
        Assertions.assertEquals(List.of("cute,blue", "avatar"), tagFilters.getValue());
    }

    @Test
    void getSquare_shouldReturnDirectCdnUrlForPublicStorageImages() {
        Page<ImageVO> page = new Page<>(1, 30);
        ImageVO vo = new ImageVO();
        vo.setId(7L);
        vo.setUuid("img-public-uuid");
        vo.setVisibility("PUBLIC");
        vo.setStorageKey("images/4/summer.png");
        vo.setMediaVersion(5L);
        page.setRecords(List.of(vo));
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("random"), eq("square")))
                .thenReturn(page);

        String cdnUrl = "https://cdn.image-space.app/public/images/4/summer.png?v=5";
        when(imageUrlService.getPublicImageUrl("images/4/summer.png", 5L)).thenReturn(cdnUrl);

        Page<ImageVO> result;
        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(false);
            result = service.getSquare(1, 30, null, null, null, null, null, null);
        }

        assertEquals(cdnUrl, result.getRecords().get(0).getImageUrl());
        assertEquals(cdnUrl, result.getRecords().get(0).getPublicUrl());
        verify(imageUrlService).getPublicImageUrl("images/4/summer.png", 5L);
        verify(storageService, never()).getPresignedUrl(any(), any(), any());
    }

    @Test
    void getSquare_shouldMarkOtherUsersImagesNotEditable() {
        Page<ImageVO> page = new Page<>(1, 30);
        ImageVO vo = new ImageVO();
        vo.setId(7L);
        vo.setUserId(42L);
        vo.setUuid("img-public-uuid");
        vo.setVisibility("PUBLIC");
        page.setRecords(List.of(vo));
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("random"), eq("square")))
                .thenReturn(page);
        when(permissionService.isOwner(42L)).thenReturn(false);
        when(permissionService.canEdit(42L)).thenReturn(false);

        Page<ImageVO> result;
        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(true);
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            result = service.getSquare(1, 30, null, null, null, null, null, null);
        }

        assertFalse(result.getRecords().get(0).getEditableByMe());
        assertFalse(result.getRecords().get(0).getOwnedByMe());
    }

    @Test
    void getSquare_shouldKeepOwnershipSeparateFromAdminEditPermission() {
        Page<ImageVO> page = new Page<>(1, 30);
        ImageVO vo = new ImageVO();
        vo.setId(7L);
        vo.setUserId(42L);
        vo.setUuid("img-public-uuid");
        vo.setVisibility("PUBLIC");
        page.setRecords(List.of(vo));
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("random"), eq("square")))
                .thenReturn(page);
        when(permissionService.isOwner(42L)).thenReturn(false);
        when(permissionService.canEdit(42L)).thenReturn(true);

        Page<ImageVO> result;
        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(true);
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            result = service.getSquare(1, 30, null, null, null, null, null, null);
        }

        assertFalse(result.getRecords().get(0).getOwnedByMe());
        Assertions.assertTrue(result.getRecords().get(0).getEditableByMe());
    }

    @Test
    void getPublicByUserUuid_shouldFilterToThatUsersPublicImages() {
        User owner = new User();
        owner.setId(42L);
        owner.setUuid("owner-uuid");
        when(userMapper.selectOne(any())).thenReturn(owner);
        when(imageMapper.selectImageVOPage(any(), eq(42L), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("latest"), isNull()))
                .thenReturn(new Page<ImageVO>(1, 30));

        service.getPublicByUserUuid("owner-uuid", 1, 30, null, null, "latest", null, null, null);

        verify(imageMapper).selectImageVOPage(any(), eq(42L), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("latest"), isNull());
    }
}
