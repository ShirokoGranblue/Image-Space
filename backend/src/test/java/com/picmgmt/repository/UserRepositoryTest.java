package com.picmgmt.repository;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock private UserMapper userMapper;
    @Mock private CacheService cacheService;
    @Mock private MediaUrlUtil mediaUrlUtil;

    @Test
    void toVO_shouldExposeAvatarAndBackgroundThroughCdnUrlsWithVersion() {
        UserRepository repository = new UserRepository(userMapper, cacheService, mediaUrlUtil);
        User user = new User();
        user.setId(4L);
        user.setUsername("alice");
        user.setAvatarKey("4/avatar.png");
        user.setBackgroundKey("4/background.jpg");

        String expectedAvatarUrl = "https://cdn.image-space.app/4/avatar.png?v=867e55914ef4";
        String expectedBackgroundUrl = "https://cdn.image-space.app/4/background.jpg?v=326f74a30ecc";
        when(mediaUrlUtil.userMediaUrl("4/avatar.png")).thenReturn(expectedAvatarUrl);
        when(mediaUrlUtil.userMediaUrl("4/background.jpg")).thenReturn(expectedBackgroundUrl);

        UserVO vo = repository.toVO(user);

        assertEquals(expectedAvatarUrl, vo.getAvatarUrl());
        assertEquals(expectedAvatarUrl, vo.getAvatar());
        assertEquals(expectedBackgroundUrl, vo.getBackgroundUrl());
        assertEquals(expectedBackgroundUrl, vo.getBackground());
    }
}
