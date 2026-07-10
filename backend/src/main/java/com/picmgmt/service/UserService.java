package com.picmgmt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.entity.User;
import com.picmgmt.dto.CodeLoginDTO;
import com.picmgmt.vo.UserVO;

public interface UserService {

    UserVO register(RegisterDTO dto);

    String login(LoginDTO dto);

    void logout();

    User getById(Long id);

    User getByUuid(String uuid);

    UserVO getUserVOById(Long id);

    UserVO getUserVOByUuid(String uuid);

    UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio);

    UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio, String emailCode);

    void sendEmailChangeCode(Long userId, String email);

    void updateAvatar(Long userId, String avatarPath);

    void updateBackground(Long userId, String backgroundPath);

    Page<UserVO> getUserList(Integer page, Integer limit);

    void checkField(String field, String value, Long excludeId);

    void sendCode(String email, String captchaId, String captchaCode, String purpose);

    String loginByCode(CodeLoginDTO dto);

    void deleteAccount(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
