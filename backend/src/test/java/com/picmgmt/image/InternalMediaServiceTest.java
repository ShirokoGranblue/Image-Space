package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.auth.SaTokenPermissionImpl;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.vo.MediaMetaVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalMediaServiceTest {

    @Mock private ImageMapper imageMapper;
    @Mock private ImageUrlService imageUrlService;
    @Mock private ImagePermissionService permissionService;
    @Mock private SaTokenPermissionImpl roleService;

    @Test
    void getMeta_shouldExposeWorkerMediaShape() {
        Image image = image("PUBLIC", 4L);
        when(imageMapper.selectOne(any())).thenReturn(image);
        InternalMediaService service = new InternalMediaService(imageMapper, imageUrlService, permissionService, roleService);

        MediaMetaVO meta = service.getMeta("images/a.png");

        assertEquals(7L, meta.getImageId());
        assertEquals("images/a.png", meta.getStorageKey());
        assertEquals("public", meta.getVisibility());
        assertEquals(4L, meta.getOwnerId());
        assertEquals(4L, meta.getVersion());
    }

    @Test
    void authorize_shouldAcceptValidShortToken() {
        Image image = image("PRIVATE", 1L);
        when(imageMapper.selectOne(any())).thenReturn(image);
        when(imageUrlService.authorizePrivateAccess("images/a.png", "token")).thenReturn(true);
        InternalMediaService service = new InternalMediaService(imageMapper, imageUrlService, permissionService, roleService);

        assertTrue(service.authorize("images/a.png", "token", null, null));
    }

    @Test
    void authorize_shouldAcceptViewerTokenWhenPermissionAllows() {
        Image image = image("SPECIFIED", 1L);
        when(imageMapper.selectOne(any())).thenReturn(image);
        when(roleService.getRoleList(5L, "login")).thenReturn(List.of("user"));
        when(permissionService.canViewAsUser(image, 5L, false)).thenReturn(true);
        InternalMediaService service = new InternalMediaService(imageMapper, imageUrlService, permissionService, roleService);

        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(() -> StpUtil.getLoginIdByToken("satoken-value")).thenReturn(5L);
            assertTrue(service.authorize("images/a.png", null, null, "satoken-value"));
        }

        verify(permissionService).canViewAsUser(image, 5L, false);
    }

    private Image image(String visibility, Long mediaVersion) {
        Image image = new Image();
        image.setId(7L);
        image.setUserId(4L);
        image.setStorageKey("images/a.png");
        image.setVisibility(visibility);
        image.setMediaVersion(mediaVersion);
        return image;
    }
}
