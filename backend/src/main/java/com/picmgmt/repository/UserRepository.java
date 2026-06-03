package com.picmgmt.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;
    private final CacheService cacheService;
    private final MediaUrlUtil mediaUrlUtil;

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "user:entity:";

    public Optional<User> findById(Long id) {
        String key = KEY_PREFIX + id;
        return Optional.ofNullable(cacheService.getOrLoad(key, User.class,
                () -> userMapper.selectById(id), TTL));
    }

    public Optional<User> findByUuid(String uuid) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUuid, uuid));
        return Optional.ofNullable(user);
    }

    public void updateById(User user) {
        userMapper.updateById(user);
        cacheService.evict(KEY_PREFIX + user.getId());
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUuid(user.getUuid());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setBio(user.getBio());
        vo.setCreateTime(user.getCreateTime());

        String avatarUrl = user.getAvatarKey() != null
                ? mediaUrlUtil.userAvatarUrl(user.getUuid(), user.getAvatarKey())
                : user.getAvatar();
        String backgroundUrl = user.getBackgroundKey() != null
                ? mediaUrlUtil.userBackgroundUrl(user.getUuid(), user.getBackgroundKey())
                : user.getBackground();
        vo.setAvatar(avatarUrl);
        vo.setAvatarUrl(avatarUrl);
        vo.setBackground(backgroundUrl);
        vo.setBackgroundUrl(backgroundUrl);
        return vo;
    }
}
