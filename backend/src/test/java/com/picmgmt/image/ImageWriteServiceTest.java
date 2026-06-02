package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageWriteServiceTest {

    @Mock private ImageRepository imageRepository;
    @Mock private StorageService storageService;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ImagePermissionService permissionService;
    @Mock private ImageUrlService imageUrlService;
    @Mock private MediaMetaCacheService mediaMetaCacheService;
    @Mock private CloudflareCachePurgeService cloudflareCachePurgeService;

    private ImageWriteService service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new ImageWriteService(
                imageRepository,
                storageService,
                categoryMapper,
                permissionService,
                imageUrlService,
                mediaMetaCacheService,
                cloudflareCachePurgeService
        );
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void upload_shouldUseCustomNameBodyAndKeepOriginalExtension() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
        when(storageService.upload(eq("images"), any(String.class), any(byte[].class), eq("image/jpeg"),
                eq(ImageUrlService.PUBLIC_CACHE_CONTROL)))
                .thenReturn("4/test.jpg");
        doAnswer(invocation -> {
            var image = invocation.getArgument(0, com.picmgmt.entity.Image.class);
            ImageVO vo = new ImageVO();
            vo.setImageName(image.getImageName());
            return vo;
        }).when(imageRepository).toVO(any());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original.jpg",
                "image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        ImageVO result = service.upload(
                file, null, null, null, "PUBLIC", null, "evil.png"
        );

        assertEquals("evil.jpg", result.getImageName());
    }

    @Test
    void upload_shouldAcceptGifAndUseGifMimeType() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
        when(storageService.upload(eq("images"), any(String.class), any(byte[].class), eq("image/gif"),
                eq(ImageUrlService.PUBLIC_CACHE_CONTROL)))
                .thenReturn("4/test.gif");
        doAnswer(invocation -> {
            var image = invocation.getArgument(0, com.picmgmt.entity.Image.class);
            ImageVO vo = new ImageVO();
            vo.setImageName(image.getImageName());
            vo.setImageType(image.getImageType());
            return vo;
        }).when(imageRepository).toVO(any());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loop.gif",
                "image/gif",
                new byte[] {'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00}
        );

        ImageVO result = service.upload(
                file, null, null, null, "PUBLIC", null, null
        );

        assertEquals("loop.gif", result.getImageName());
        assertEquals("GIF", result.getImageType());
    }

    @Test
    void upload_shouldDefaultToPublicWhenVisibilityIsOmitted() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
        when(storageService.upload(eq("images"), any(String.class), any(byte[].class), eq("image/png"),
                eq(ImageUrlService.PUBLIC_CACHE_CONTROL)))
                .thenReturn("images/test.png");
        doAnswer(invocation -> {
            var image = invocation.getArgument(0, com.picmgmt.entity.Image.class);
            ImageVO vo = new ImageVO();
            vo.setVisibility(image.getVisibility());
            return vo;
        }).when(imageRepository).toVO(any());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "public.png",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        ImageVO result = service.upload(
                file, null, null, null, null, null, null
        );

        assertEquals("PUBLIC", result.getVisibility());
    }

    @Test
    void update_shouldIncrementVersionAndClearPrivateAccessWhenPrivateBecomesPublic() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
        var image = image("PRIVATE", 2L);
        when(imageRepository.findById(7L)).thenReturn(java.util.Optional.of(image));
        doAnswer(invocation -> {
            var updated = invocation.getArgument(0, com.picmgmt.entity.Image.class);
            ImageVO vo = new ImageVO();
            vo.setVisibility(updated.getVisibility());
            vo.setMediaVersion(updated.getMediaVersion());
            return vo;
        }).when(imageRepository).toVO(any());

        ImageUpdateDTO dto = new ImageUpdateDTO();
        dto.setVisibility("PUBLIC");

        ImageVO result = service.update(7L, dto);

        assertEquals("PUBLIC", result.getVisibility());
        assertEquals(3L, result.getMediaVersion());
        assertNotNull(image.getUploadTime());
        verify(imageUrlService).evictPrivateAccess("images/a.png");
        verify(mediaMetaCacheService).evict("images/a.png");
        verify(storageService).updateObjectMetadata(
                "images",
                "images/a.png",
                ImageUrlService.PUBLIC_CACHE_CONTROL,
                true
        );
        verify(cloudflareCachePurgeService, never()).purgeFile(any());
    }

    @Test
    void update_shouldPurgeOldPublicUrlWhenPublicBecomesPrivate() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
        var image = image("PUBLIC", 5L);
        when(imageRepository.findById(7L)).thenReturn(java.util.Optional.of(image));
        when(imageUrlService.getPublicImageUrl("images/a.png", 5L))
                .thenReturn("https://cdn.image-space.app/public/images/a.png?v=5");
        doAnswer(invocation -> {
            var updated = invocation.getArgument(0, com.picmgmt.entity.Image.class);
            ImageVO vo = new ImageVO();
            vo.setVisibility(updated.getVisibility());
            vo.setMediaVersion(updated.getMediaVersion());
            return vo;
        }).when(imageRepository).toVO(any());

        ImageUpdateDTO dto = new ImageUpdateDTO();
        dto.setVisibility("PRIVATE");

        ImageVO result = service.update(7L, dto);

        assertEquals("PRIVATE", result.getVisibility());
        assertEquals(5L, result.getMediaVersion());
        verify(imageUrlService).evictPrivateAccess("images/a.png");
        verify(mediaMetaCacheService).evict("images/a.png");
        verify(storageService).updateObjectMetadata(
                "images",
                "images/a.png",
                ImageUrlService.PRIVATE_CACHE_CONTROL,
                false
        );
        verify(cloudflareCachePurgeService).purgeFile("https://cdn.image-space.app/public/images/a.png?v=5");
    }

    private com.picmgmt.entity.Image image(String visibility, Long mediaVersion) {
        com.picmgmt.entity.Image image = new com.picmgmt.entity.Image();
        image.setId(7L);
        image.setUuid("img-uuid");
        image.setUserId(4L);
        image.setStorageKey("images/a.png");
        image.setImageName("a.png");
        image.setVisibility(visibility);
        image.setMediaVersion(mediaVersion);
        return image;
    }
}
