package com.picmgmt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.entity.User;
import com.picmgmt.vo.UserVO;

public interface UserService {

    UserVO register(RegisterDTO dto);

    String login(LoginDTO dto);

    void logout();

    User getById(Long id);

    UserVO getUserVOById(Long id);

    UserVO updateProfile(Long userId, String displayName, String email, String phone, String bio);

    void updateAvatar(Long userId, String avatarPath);

    void updateBackground(Long userId, String backgroundPath);

    Page<UserVO> getUserList(Integer page, Integer limit);
}
