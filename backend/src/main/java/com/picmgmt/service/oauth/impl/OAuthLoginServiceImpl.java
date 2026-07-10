package com.picmgmt.service.oauth.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.auth.UserRole;
import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.oauth.MicrosoftUserInfo;
import com.picmgmt.dto.oauth.OAuthLoginResult;
import com.picmgmt.entity.User;
import com.picmgmt.entity.UserOauthAccount;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.mapper.UserOauthAccountMapper;
import com.picmgmt.service.oauth.OAuthLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * OAuth 登录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginServiceImpl implements OAuthLoginService {

    private final UserMapper userMapper;
    private final UserOauthAccountMapper oauthAccountMapper;
    private final BloomFilterService bloomFilterService;
    private final UserRoleMapper userRoleMapper;

    private static final String PROVIDER_MICROSOFT = "microsoft";

    @Override
    @Transactional
    public OAuthLoginResult loginOrRegisterByMicrosoft(MicrosoftUserInfo userInfo) {
        String providerUserId = userInfo.getId();
        // 使用经过 Microsoft 验证的邮箱（UPN 优先）进行账号绑定，
        // 防止通过未验证的 mail 别名字段绑定到他人账号
        String verifiedEmail = userInfo.getVerifiedEmail();
        String displayName = userInfo.getDisplayName();

        // 1. 先查是否已有绑定
        UserOauthAccount existingBinding = oauthAccountMapper.selectOne(
                new LambdaQueryWrapper<UserOauthAccount>()
                        .eq(UserOauthAccount::getProvider, PROVIDER_MICROSOFT)
                        .eq(UserOauthAccount::getProviderUserId, providerUserId)
        );

        if (existingBinding != null) {
            // 绑定存在，直接登录
            User user = userMapper.selectById(existingBinding.getUserId());
            if (user == null) {
                log.error("Microsoft OAuth: binding exists but user not found, userId={}", existingBinding.getUserId());
                throw new RuntimeException("用户不存在");
            }

            // 检查用户是否被删除
            if (user.getDeleted() != null && user.getDeleted() == 1) {
                log.warn("Microsoft OAuth: user is deleted, userId={}", user.getId());
                throw new RuntimeException("用户已注销");
            }

            // 登录
            StpUtil.login(user.getId());
            log.info("Microsoft OAuth login: existing binding, userId={}", user.getId());

            return OAuthLoginResult.builder()
                    .userId(user.getId())
                    .tokenName(StpUtil.getTokenName())
                    .tokenValue(StpUtil.getTokenValue())
                    .newlyCreated(false)
                    .build();
        }

        // 2. 没有绑定，尝试根据已验证邮箱查找已有用户
        //    使用 getVerifiedEmail()（优先 UPN）而非 getEmail()（优先 mail），
        //    因为 UPN 是 Microsoft 的登录凭据，始终经过验证；
        //    mail 可能是用户自行添加的别名，验证强度不足。
        User existingUser = null;
        if (verifiedEmail != null && !verifiedEmail.isBlank()) {
            existingUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, verifiedEmail)
                            .and(w -> w.isNull(User::getDeleted).or().eq(User::getDeleted, 0))
            );
        }

        Long userId;
        boolean newlyCreated = false;

        if (existingUser != null) {
            if (!Integer.valueOf(1).equals(existingUser.getEmailVerified())) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "该邮箱已存在但尚未验证，请先使用邮箱验证码登录后再绑定 Microsoft"
                );
            }
            // 找到已有用户，创建绑定
            userId = existingUser.getId();
            createOAuthBinding(userId, userInfo);
            log.info("Microsoft OAuth: created binding for existing user, userId={}, verifiedEmail={}", userId, verifiedEmail);
        } else {
            // 没有已有用户，创建新用户
            User newUser = createNewUser(userInfo);
            userMapper.insert(newUser);
            bloomFilterService.addUser(newUser.getId());
            UserRole userRole = new UserRole();
            userRole.setUserId(newUser.getId());
            userRole.setRoleId(3L);
            userRoleMapper.insert(userRole);
            userId = newUser.getId();
            newlyCreated = true;

            // 创建绑定
            createOAuthBinding(userId, userInfo);
            log.info("Microsoft OAuth: created new user, userId={}, username={}", userId, newUser.getUsername());
        }

        // 3. 登录
        User user = userMapper.selectById(userId);
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            log.warn("Microsoft OAuth: newly created/found user is deleted, userId={}", userId);
            throw new RuntimeException("用户已注销");
        }

        StpUtil.login(userId);

        return OAuthLoginResult.builder()
                .userId(userId)
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .newlyCreated(newlyCreated)
                .build();
    }

    /**
     * 创建 OAuth 绑定
     */
    private void createOAuthBinding(Long userId, MicrosoftUserInfo userInfo) {
        UserOauthAccount binding = new UserOauthAccount();
        binding.setUserId(userId);
        binding.setProvider(PROVIDER_MICROSOFT);
        binding.setProviderUserId(userInfo.getId());
        // 存储经过验证的邮箱（UPN 优先）
        binding.setProviderEmail(userInfo.getVerifiedEmail());
        binding.setProviderUsername(userInfo.getDisplayName());
        // Microsoft Graph /me 不直接返回头像，需要通过 /photo 端点获取
        // 这里先留空，后续可以扩展
        binding.setAvatarUrl(null);
        oauthAccountMapper.insert(binding);
    }

    /**
     * 创建新用户
     */
    private User createNewUser(MicrosoftUserInfo userInfo) {
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());

        // 生成用户名
        String baseUsername = generateUsername(userInfo);
        String username = baseUsername;
        int suffix = 1;
        while (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            username = baseUsername + "_" + suffix++;
        }
        user.setUsername(username);

        // 设置显示名
        if (userInfo.getDisplayName() != null && !userInfo.getDisplayName().isBlank()) {
            user.setDisplayName(userInfo.getDisplayName());
        } else {
            user.setDisplayName(username);
        }

        // 设置随机不可登录密码
        user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));

        // 设置角色
        user.setRole("user");

        // 设置已验证邮箱（UPN 优先）
        user.setEmail(userInfo.getVerifiedEmail());
        user.setEmailVerified(userInfo.getVerifiedEmail() == null ? 0 : 1);

        // 设置未删除状态
        user.setDeleted(0);

        return user;
    }

    /**
     * 生成用户名
     */
    private String generateUsername(MicrosoftUserInfo userInfo) {
        // 优先使用 displayName
        if (userInfo.getDisplayName() != null && !userInfo.getDisplayName().isBlank()) {
            // 清理 displayName，只保留合法字符
            String cleaned = userInfo.getDisplayName()
                    .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
            if (!cleaned.isBlank() && cleaned.length() >= 2) {
                return "ms_" + cleaned.substring(0, Math.min(cleaned.length(), 30));
            }
        }

        // 使用邮箱前缀
        String email = userInfo.getEmail();
        if (email != null && !email.isBlank() && email.contains("@")) {
            String prefix = email.split("@")[0];
            prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "_");
            if (prefix.length() >= 2) {
                return "ms_" + prefix.substring(0, Math.min(prefix.length(), 30));
            }
        }

        // 最后使用 microsoft_user_ + 短ID
        return "microsoft_user_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
