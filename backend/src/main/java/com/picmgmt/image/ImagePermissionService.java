package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ImagePermissionService {

    private final UserMapper userMapper;

    public boolean canView(Image image) {
        if ("PUBLIC".equals(image.getVisibility())) return true;
        if (!StpUtil.isLogin()) return false;

        long userId = StpUtil.getLoginIdAsLong();
        if (image.getUserId().equals(userId) || StpUtil.hasRole("admin")) return true;
        if (!"SPECIFIED".equals(image.getVisibility())) return false;

        User viewer = userMapper.selectById(userId);
        if (viewer == null || image.getVisibleUsernames() == null) return false;

        return Arrays.stream(image.getVisibleUsernames().split(","))
                .map(String::trim)
                .anyMatch(viewer.getUsername()::equals);
    }

    public void validateOwnershipOrAdmin(Image image) {
        long userId = StpUtil.getLoginIdAsLong();
        if (!image.getUserId().equals(userId) && !StpUtil.hasRole("admin")) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
    }

    public boolean canEdit(Long ownerUserId) {
        if (!StpUtil.isLogin()) return false;
        long userId = StpUtil.getLoginIdAsLong();
        return ownerUserId != null && (ownerUserId.equals(userId) || StpUtil.hasRole("admin"));
    }
}
