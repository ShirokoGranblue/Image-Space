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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageWriteServiceTest {

    @Mock private ImageRepository imageRepository;
    @Mock private StorageService storageService;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ImagePermissionService permissionService;
    @Mock private ImageUrlService imageUrlService;

    private ImageWriteService service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new ImageWriteService(imageRepository, storageService, categoryMapper, permissionService, imageUrlService);
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
}
