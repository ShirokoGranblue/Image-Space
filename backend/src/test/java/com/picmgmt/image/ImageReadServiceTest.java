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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void getSquare_shouldDecorateLikeAndCommentCountsInBatches() {
        Page<ImageVO> page = new Page<>(1, 30);
        ImageVO first = new ImageVO();
        first.setId(7L);
        first.setVisibility("PUBLIC");
        ImageVO second = new ImageVO();
        second.setId(8L);
        second.setVisibility("PUBLIC");
        page.setRecords(List.of(first, second));
        when(imageMapper.selectImageVOPage(any(), isNull(), isNull(), isNull(), eq("PUBLIC"),
                isNull(), eq("upload_time"), eq("desc"), eq("random"), eq("square")))
                .thenReturn(page);
        when(imageMapper.countByImageIds(List.of(7L, 8L))).thenReturn(List.of(
                Map.<String, Object>of("image_id", 7L, "cnt", 3L)
        ));
        when(imageMapper.countCommentsByImageIds(List.of(7L, 8L))).thenReturn(List.of(
                Map.<String, Object>of("image_id", 7L, "cnt", 2L),
                Map.<String, Object>of("image_id", 8L, "cnt", 5L)
        ));

        Page<ImageVO> result;
        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(false);
            result = service.getSquare(1, 30, null, null, null, null, null, null);
        }

        assertEquals(3L, result.getRecords().get(0).getLikeCount());
        assertEquals(2L, result.getRecords().get(0).getCommentCount());
        assertEquals(0L, result.getRecords().get(1).getLikeCount());
        assertEquals(5L, result.getRecords().get(1).getCommentCount());
        verify(imageMapper).countByImageIds(List.of(7L, 8L));
        verify(imageMapper).countCommentsByImageIds(List.of(7L, 8L));
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

    @Test
    void downloadByUuid_shouldUseOriginalKeyInsteadOfDisplayVariantKeys() {
        com.picmgmt.entity.Image image = publicImage();
        image.setStorageKey("images/img-uuid/original.png");
        image.setOriginalKey("images/img-uuid/original.png");
        image.setMediumKey("images/img-uuid/medium.jpg");
        image.setThumbKey("images/img-uuid/thumb.jpg");
        byte[] originalBytes = new byte[] {1, 2, 3};
        when(imageRepository.findByUuid("img-uuid")).thenReturn(Optional.of(image));
        when(permissionService.canView(image)).thenReturn(true);
        when(storageService.download("images", "images/img-uuid/original.png")).thenReturn(originalBytes);

        byte[] result = service.downloadByUuid("img-uuid");

        assertArrayEquals(originalBytes, result);
        verify(storageService).download("images", "images/img-uuid/original.png");
        verify(storageService, never()).download("images", "images/img-uuid/thumb.jpg");
        verify(storageService, never()).download("images", "images/img-uuid/medium.jpg");
    }

    @Test
    void downloadByUuidWithFormat_shouldConvertOriginalToJpgAndCacheResult() {
        com.picmgmt.entity.Image image = publicImage();
        image.setOriginalKey("images/img-uuid/original.png");
        image.setOriginalFilename("transparent.png");
        image.setOriginalContentType("image/png");
        image.setOriginalExt("png");
        when(imageRepository.findByUuid("img-uuid")).thenReturn(Optional.of(image));
        when(permissionService.canView(image)).thenReturn(true);
        when(storageService.objectExists("images", "images/img-uuid/download/original.jpg")).thenReturn(false);
        when(storageService.download("images", "images/img-uuid/original.png")).thenReturn(pngBytes());

        ImageDownloadFile result = service.downloadByUuid("img-uuid", "jpeg");

        assertEquals("image/jpeg", result.contentType());
        assertEquals("transparent.jpg", result.filename());
        Assertions.assertTrue(result.bytes().length > 2);
        assertEquals((byte) 0xFF, result.bytes()[0]);
        verify(storageService).upload(eq("images"), eq("images/img-uuid/download/original.jpg"),
                any(byte[].class), eq("image/jpeg"), eq(ImageUrlService.PRIVATE_CACHE_CONTROL));
    }

    @Test
    void downloadByUuidWithFormat_shouldRejectStaticImageToGif() {
        com.picmgmt.entity.Image image = publicImage();
        image.setOriginalKey("images/img-uuid/original.png");
        image.setOriginalContentType("image/png");
        image.setOriginalExt("png");
        when(imageRepository.findByUuid("img-uuid")).thenReturn(Optional.of(image));
        when(permissionService.canView(image)).thenReturn(true);
        when(storageService.download("images", "images/img-uuid/original.png")).thenReturn(pngBytes());

        com.picmgmt.common.BusinessException ex = assertThrows(
                com.picmgmt.common.BusinessException.class,
                () -> service.downloadByUuid("img-uuid", "gif")
        );

        assertEquals(com.picmgmt.common.ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verify(storageService, never()).upload(eq("images"), any(String.class), any(byte[].class), any(String.class), any(String.class));
    }

    private com.picmgmt.entity.Image publicImage() {
        com.picmgmt.entity.Image image = new com.picmgmt.entity.Image();
        image.setId(7L);
        image.setUuid("img-uuid");
        image.setUserId(4L);
        image.setVisibility("PUBLIC");
        image.setMediaVersion(1L);
        return image;
    }

    private static byte[] pngBytes() {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, new Color(255, 0, 0, 0).getRGB());
            image.setRGB(1, 0, Color.BLUE.getRGB());
            image.setRGB(0, 1, Color.GREEN.getRGB());
            image.setRGB(1, 1, Color.WHITE.getRGB());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
