package com.picmgmt.repository;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;
    private final StorageService storageService;
    private final CacheService cacheService;

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "user:entity:";

    public Optional<User> findById(Long id) {
        String key = KEY_PREFIX + id;
        return cacheService.get(key, User.class)
                .or(() -> {
                    User user = userMapper.selectById(id);
                    if (user != null) {
                        cacheService.put(key, user, TTL);
                    }
                    return Optional.ofNullable(user);
                });
    }

    public void updateById(User user) {
        userMapper.updateById(user);
        cacheService.evict(KEY_PREFIX + user.getId());
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setBio(user.getBio());
        vo.setCreateTime(user.getCreateTime());

        String avatarUrl = user.getAvatarKey() != null ? "/api/user/avatar/" + user.getId() : user.getAvatar();
        String backgroundUrl = user.getBackgroundKey() != null ? "/api/user/background/" + user.getId() : user.getBackground();
        vo.setAvatar(avatarUrl);
        vo.setAvatarUrl(avatarUrl);
        vo.setBackground(backgroundUrl);
        vo.setBackgroundUrl(backgroundUrl);
        return vo;
    }
}
