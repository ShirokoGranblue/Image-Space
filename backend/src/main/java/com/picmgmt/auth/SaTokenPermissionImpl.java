package com.picmgmt.auth;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SaTokenPermissionImpl implements StpInterface {

    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(Long.parseLong(loginId.toString()));
        if (roleIds.isEmpty()) return List.of();
        List<Long> permIds = rolePermissionMapper.selectPermIdsByRoleIds(roleIds);
        if (permIds.isEmpty()) return List.of();
        return permissionMapper.selectBatchIds(permIds).stream()
                .map(Permission::getCode)
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(Long.parseLong(loginId.toString()));
        if (roleIds.isEmpty()) return List.of();
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getCode)
                .toList();
    }
}
