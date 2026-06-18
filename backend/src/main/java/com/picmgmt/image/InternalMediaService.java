package com.picmgmt.image;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.auth.SaTokenPermissionImpl;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.vo.MediaMetaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalMediaService {

    private final ImageMapper imageMapper;
    private final ImageUrlService imageUrlService;
    private final ImagePermissionService permissionService;
    private final SaTokenPermissionImpl roleService;

    public MediaMetaVO getMeta(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        Image image = findByStorageKey(normalizedKey);
        if (image == null) {
            return null;
        }
        return new MediaMetaVO(
                image.getId(),
                normalizedKey,
                "PUBLIC".equals(image.getVisibility()) ? "public" : "private",
                image.getUserId(),
                normalizeVersion(image.getMediaVersion())
        );
    }

    public boolean authorize(String storageKey, String accessToken, String authorization, String saToken) {
        String normalizedKey = normalizeKey(storageKey);
        Image image = findByStorageKey(normalizedKey);
        if (image == null) {
            log.debug("Internal media authorize rejected missing image: {}", storageKey);
            return false;
        }

        if (accessToken != null && !accessToken.isBlank()
                && imageUrlService.authorizePrivateAccess(normalizedKey, accessToken)) {
            return true;
        }

        Long viewerUserId = resolveViewerUserId(authorization, saToken);
        if (viewerUserId == null) {
            return false;
        }
        boolean admin = isAdmin(viewerUserId);
        return permissionService.canViewAsUser(image, viewerUserId, admin);
    }

    private Image findByStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || !storageKey.startsWith("images/")) {
            return null;
        }
        return imageMapper.selectOne(new LambdaQueryWrapper<Image>()
                .and(wrapper -> wrapper
                        .eq(Image::getStorageKey, storageKey)
                        .or().eq(Image::getOriginalKey, storageKey)
                        .or().eq(Image::getMediumKey, storageKey)
                        .or().eq(Image::getThumbKey, storageKey)));
    }

    private Long resolveViewerUserId(String authorization, String saToken) {
        String token = normalizeRequestToken(authorization, saToken);
        if (token == null) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId == null ? null : Long.parseLong(loginId.toString());
        } catch (NotLoginException | NumberFormatException e) {
            return null;
        }
    }

    private boolean isAdmin(Long userId) {
        try {
            List<String> roles = roleService.getRoleList(userId, "login");
            return roles.contains("admin");
        } catch (Exception e) {
            log.debug("Failed to resolve roles for media authorize user {}", userId, e);
            return false;
        }
    }

    private String normalizeRequestToken(String authorization, String saToken) {
        if (saToken != null && !saToken.isBlank()) {
            return saToken.trim();
        }
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }

    private Long normalizeVersion(Long mediaVersion) {
        return mediaVersion == null || mediaVersion < 1 ? 1L : mediaVersion;
    }

    private String normalizeKey(String storageKey) {
        return storageKey == null ? "" : storageKey.replaceAll("^/+", "");
    }
}
