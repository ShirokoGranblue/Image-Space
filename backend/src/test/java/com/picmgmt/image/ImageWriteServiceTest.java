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

    private ImageWriteService service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new ImageWriteService(imageRepository, storageService, categoryMapper, permissionService);
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void upload_shouldUseCustomNameBodyAndKeepOriginalExtension() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
        when(storageService.upload(eq("images"), any(String.class), any(byte[].class), eq("image/jpeg")))
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
}
