package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.repository.UserRepository;
import com.picmgmt.service.UserService;
import com.picmgmt.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

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
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setDisplayName(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setRole("user");
        userMapper.insert(user);
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
        return userRepository.toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (displayName != null) user.setDisplayName(displayName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
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
}
