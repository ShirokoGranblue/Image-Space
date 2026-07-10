package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.auth.UserRole;
import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.cache.RedisCacheService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.CodeLoginDTO;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.repository.UserRepository;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.EmailService;
import com.picmgmt.service.UserService;
import com.picmgmt.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;
    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final UserRoleMapper userRoleMapper;
    private final BloomFilterService bloomFilterService;
    private final com.picmgmt.storage.StorageService storageService;

    @Override
    @Transactional
    public UserVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        String email = dto.getEmail().trim();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            String phone = dto.getPhone().trim();
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }
        verifyEmailCode("register", email, dto.getCode());
        User user = new User();
        user.setUuid(java.util.UUID.randomUUID().toString());
        user.setUsername(dto.getUsername());
        user.setDisplayName(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setRole("user");
        user.setEmail(email);
        user.setEmailVerified(1);
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            user.setPhone(dto.getPhone().trim());
        }
        userMapper.insert(user);
        bloomFilterService.addUser(user.getId());
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        userRoleMapper.insert(userRole);
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    @Override
    public String login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User getByUuid(String uuid) {
        return userRepository.findByUuid(uuid).orElse(null);
    }

    @Override
    public UserVO getUserVOById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
        return userRepository.toVO(user);
    }

    @Override
    public UserVO getUserVOByUuid(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
        return userRepository.toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio) {
        return updateProfile(userId, displayName, email, phone, bio, null);
    }

    @Override
    public UserVO updateProfile(Long userId, String displayName, String email, String phone,
                                String bio, String emailCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (displayName != null) user.setDisplayName(displayName);
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                user.setEmail(null);
                user.setEmailVerified(0);
            } else if (!email.equals(user.getEmail())
                    || !Objects.equals(user.getEmailVerified(), 1) && hasText(emailCode)) {
                if (userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getEmail, email)
                                .ne(User::getId, userId)) > 0) {
                    throw new BusinessException(ErrorCode.EMAIL_EXISTS);
                }
                verifyEmailChangeCode(userId, email, emailCode);
                user.setEmail(email);
                user.setEmailVerified(1);
            }
        }
        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) {
                user.setPhone(null);
            } else if (!phone.equals(user.getPhone())) {
                if (userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
                                .ne(User::getId, userId)) > 0) {
                    throw new BusinessException(ErrorCode.PHONE_EXISTS);
                }
                user.setPhone(phone);
            }
        }
        if (bio != null) user.setBio(bio);
        userRepository.updateById(user);
        return userRepository.toVO(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String oldKey = user.getAvatarKey();
        user.setAvatarKey(avatarKey);
        try {
            userRepository.updateById(user);
        } catch (RuntimeException e) {
            safeDeleteMedia("avatars", avatarKey);
            throw e;
        }
        if (!Objects.equals(oldKey, avatarKey)) {
            safeDeleteMedia("avatars", oldKey);
        }
    }

    @Override
    public void updateBackground(Long userId, String backgroundKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String oldKey = user.getBackgroundKey();
        user.setBackgroundKey(backgroundKey);
        try {
            userRepository.updateById(user);
        } catch (RuntimeException e) {
            safeDeleteMedia("backgrounds", backgroundKey);
            throw e;
        }
        if (!Objects.equals(oldKey, backgroundKey)) {
            safeDeleteMedia("backgrounds", oldKey);
        }
    }

    private void safeDeleteMedia(String bucket, String objectKey) {
        try {
            storageService.deleteObjectIfExists(bucket, objectKey);
        } catch (RuntimeException e) {
            log.warn("Failed to clean up {} object {}: {}", bucket, objectKey, e.getMessage());
        }
    }

    @Override
    public Page<UserVO> getUserList(Integer page, Integer limit) {
        Page<User> pageParam = new Page<>(page, limit);
        Page<User> userPage = userMapper.selectPage(pageParam,
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<UserVO> voList = userPage.getRecords().stream()
                .map(userRepository::toVO)
                .toList();
        Page<UserVO> voPage = new Page<>(page, limit, userPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void checkField(String field, String value, Long excludeId) {
        if (value == null || value.trim().isEmpty()) return;
        value = value.trim();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if ("email".equals(field)) {
            wrapper.eq(User::getEmail, value);
        } else if ("phone".equals(field)) {
            wrapper.eq(User::getPhone, value);
        }
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("email".equals(field)
                    ? ErrorCode.EMAIL_EXISTS : ErrorCode.PHONE_EXISTS);
        }
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final String CODE_PURPOSE_REGISTER = "register";
    private static final String CODE_PURPOSE_LOGIN = "login";

    @Override
    public void sendCode(String email, String captchaId, String captchaCode, String purpose) {
        String normalizedPurpose = normalizeCodePurpose(purpose);
        if (CODE_PURPOSE_REGISTER.equals(normalizedPurpose)) {
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
        } else {
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user == null) {
                throw new BusinessException(ErrorCode.EMAIL_NOT_BOUND);
            }
            if (user.getDeleted() != null && user.getDeleted() == 1) {
                throw new BusinessException(ErrorCode.USER_DELETED);
            }
        }
        String redisKey = verificationCodeKey(normalizedPurpose, email);
        String cooldownKey = verificationCooldownKey(normalizedPurpose, email);
        String attemptsKey = verificationAttemptsKey(normalizedPurpose, email);
        captchaService.verify(captchaId, captchaCode);
        if (!redisCacheService.setIfAbsent(cooldownKey, "1", Duration.ofSeconds(60))) {
            throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
        }
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        try {
            emailService.sendVerificationCode(email, code);
        } catch (Exception e) {
            redisCacheService.evict(cooldownKey);
            throw new BusinessException(ErrorCode.CODE_SEND_FAILED, e.getMessage());
        }
        redisCacheService.putExact(redisKey, code, Duration.ofSeconds(300));
        redisCacheService.putExact(attemptsKey, 0, Duration.ofSeconds(300));
    }

    @Override
    public String loginByCode(CodeLoginDTO dto) {
        String email = dto.getEmail().trim();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_BOUND);
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
        String redisKey = verificationCodeKey(CODE_PURPOSE_LOGIN, email);
        String attemptsKey = verificationAttemptsKey(CODE_PURPOSE_LOGIN, email);
        Integer attempts = redisCacheService.get(attemptsKey, Integer.class).orElse(0);
        if (attempts >= MAX_CODE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码尝试次数过多，请重新获取");
        }
        String storedCode = redisCacheService.get(redisKey, String.class).orElse(null);
        if (storedCode == null || !storedCode.equals(dto.getCode().trim())) {
            redisCacheService.putExact(attemptsKey, attempts + 1, Duration.ofSeconds(300));
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        redisCacheService.evict(redisKey);
        redisCacheService.evict(attemptsKey);
        if (!Objects.equals(user.getEmailVerified(), 1)) {
            user.setEmailVerified(1);
            userRepository.updateById(user);
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void sendEmailChangeCode(Long userId, String email) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String normalizedEmail = normalizeEmail(email);
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, normalizedEmail)
                .ne(User::getId, userId)) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        String redisKey = emailChangeCodeKey(userId, normalizedEmail);
        String cooldownKey = emailChangeCooldownKey(userId, normalizedEmail);
        if (!redisCacheService.setIfAbsent(cooldownKey, "1", Duration.ofSeconds(60))) {
            throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
        }
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        try {
            emailService.sendVerificationCode(normalizedEmail, code);
        } catch (Exception e) {
            redisCacheService.evict(cooldownKey);
            throw new BusinessException(ErrorCode.CODE_SEND_FAILED, e.getMessage());
        }
        redisCacheService.putExact(redisKey, code, Duration.ofSeconds(300));
        redisCacheService.putExact(
                emailChangeAttemptsKey(userId, normalizedEmail),
                0,
                Duration.ofSeconds(300)
        );
    }

    private void verifyEmailCode(String purpose, String email, String code) {
        String redisKey = verificationCodeKey(purpose, email);
        String attemptsKey = verificationAttemptsKey(purpose, email);
        Integer attempts = redisCacheService.get(attemptsKey, Integer.class).orElse(0);
        if (attempts >= MAX_CODE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码尝试次数过多，请重新获取");
        }
        String storedCode = redisCacheService.get(redisKey, String.class).orElse(null);
        if (storedCode == null || code == null || !storedCode.equals(code.trim())) {
            redisCacheService.putExact(attemptsKey, attempts + 1, Duration.ofSeconds(300));
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        redisCacheService.evict(redisKey);
        redisCacheService.evict(attemptsKey);
    }

    private String normalizeCodePurpose(String purpose) {
        if (CODE_PURPOSE_REGISTER.equalsIgnoreCase(String.valueOf(purpose).trim())) {
            return CODE_PURPOSE_REGISTER;
        }
        return CODE_PURPOSE_LOGIN;
    }

    private String verificationCodeKey(String purpose, String email) {
        return "code:" + purpose + ":" + email;
    }

    private String verificationAttemptsKey(String purpose, String email) {
        return "code:attempts:" + purpose + ":" + email;
    }

    private String verificationCooldownKey(String purpose, String email) {
        return "code:cooldown:" + purpose + ":" + email;
    }

    private void verifyEmailChangeCode(Long userId, String email, String code) {
        String redisKey = emailChangeCodeKey(userId, email);
        String attemptsKey = emailChangeAttemptsKey(userId, email);
        Integer attempts = redisCacheService.get(attemptsKey, Integer.class).orElse(0);
        if (attempts >= MAX_CODE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码尝试次数过多，请重新获取");
        }
        String storedCode = redisCacheService.get(redisKey, String.class).orElse(null);
        if (storedCode == null || code == null || !storedCode.equals(code.trim())) {
            redisCacheService.putExact(attemptsKey, attempts + 1, Duration.ofSeconds(300));
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        redisCacheService.evict(redisKey);
        redisCacheService.evict(attemptsKey);
    }

    private String emailChangeCodeKey(Long userId, String email) {
        return "code:change_email:" + userId + ":" + email;
    }

    private String emailChangeAttemptsKey(Long userId, String email) {
        return "code:attempts:change_email:" + userId + ":" + email;
    }

    private String emailChangeCooldownKey(Long userId, String email) {
        return "code:cooldown:change_email:" + userId + ":" + email;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim();
        if (normalized.length() > 100
                || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // Clean up storage files
        try {
            if (user.getAvatarKey() != null && !user.getAvatarKey().isBlank()) {
                storageService.delete("avatars", user.getAvatarKey());
            }
            if (user.getBackgroundKey() != null && !user.getBackgroundKey().isBlank()) {
                storageService.delete("backgrounds", user.getBackgroundKey());
            }
        } catch (Exception e) {
            log.warn("Failed to clean up storage files for user {}: {}", userId, e.getMessage());
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getDeleted, 1)
                .set(User::getDisplayName, "已注销用户")
                .set(User::getEmail, null)
                .set(User::getPhone, null)
                .set(User::getBio, null)
                .set(User::getAvatar, null)
                .set(User::getAvatarKey, null)
                .set(User::getBackground, null)
                .set(User::getBackgroundKey, null)
                .set(User::getGithubUsername, null));
        redisCacheService.evict("user:entity:" + userId);
        StpUtil.logout();
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (oldPassword == null || oldPassword.isBlank() || !BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, BCrypt.hashpw(newPassword, BCrypt.gensalt())));
        redisCacheService.evict("user:entity:" + userId);
    }

}
