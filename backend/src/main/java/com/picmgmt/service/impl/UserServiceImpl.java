package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.auth.UserRole;
import com.picmgmt.auth.UserRoleMapper;
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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;
    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final UserRoleMapper userRoleMapper;

    @Override
    public UserVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String email = dto.getEmail().trim();
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
        }
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            String phone = dto.getPhone().trim();
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setDisplayName(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setRole("user");
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            user.setPhone(dto.getPhone().trim());
        }
        userMapper.insert(user);
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
    public UserVO getUserVOById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
        return userRepository.toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (displayName != null) user.setDisplayName(displayName);
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) {
                user.setEmail(null);
            } else if (!email.equals(user.getEmail())) {
                if (userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getEmail, email)
                                .ne(User::getId, userId)) > 0) {
                    throw new BusinessException(ErrorCode.EMAIL_EXISTS);
                }
                user.setEmail(email);
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
        user.setAvatarKey(avatarKey);
        userRepository.updateById(user);
    }

    @Override
    public void updateBackground(Long userId, String backgroundKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setBackgroundKey(backgroundKey);
        userRepository.updateById(user);
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

    @Override
    public void sendCode(String email, String captchaId, String captchaCode) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_BOUND);
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
        String redisKey = "code:login:" + email;
        if (!redisCacheService.setIfAbsent(redisKey, "pending", Duration.ofSeconds(60))) {
            throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
        }
        captchaService.verify(captchaId, captchaCode);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        try {
            emailService.sendVerificationCode(email, code);
        } catch (Exception e) {
            redisCacheService.evict(redisKey);
            throw new BusinessException(ErrorCode.CODE_SEND_FAILED, e.getMessage());
        }
        redisCacheService.put(redisKey, code, Duration.ofSeconds(60));
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
        String redisKey = "code:login:" + email;
        String storedCode = redisCacheService.get(redisKey, String.class).orElse(null);
        if (storedCode == null || !storedCode.equals(dto.getCode().trim())) {
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        redisCacheService.evict(redisKey);
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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
    }

}
