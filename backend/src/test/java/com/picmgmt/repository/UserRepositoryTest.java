package com.picmgmt.repository;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock private UserMapper userMapper;
    @Mock private StorageService storageService;
    @Mock private CacheService cacheService;

    @Test
    void toVO_shouldExposeAvatarAndBackgroundThroughVersionedApiProxyUrls() {
        UserRepository repository = new UserRepository(userMapper, storageService, cacheService);
        User user = new User();
        user.setId(4L);
        user.setUsername("alice");
        user.setAvatarKey("4/avatar.png");
        user.setBackgroundKey("4/background.jpg");

        UserVO vo = repository.toVO(user);

        String expectedAvatarUrl = "/api/user/avatar/4?v=867e55914ef4";
        String expectedBackgroundUrl = "/api/user/background/4?v=326f74a30ecc";
        assertEquals(expectedAvatarUrl, vo.getAvatarUrl());
        assertEquals(expectedAvatarUrl, vo.getAvatar());
        assertEquals(expectedBackgroundUrl, vo.getBackgroundUrl());
        assertEquals(expectedBackgroundUrl, vo.getBackground());
        verifyNoInteractions(storageService);
    }
}
