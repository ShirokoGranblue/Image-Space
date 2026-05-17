package com.picmgmt.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
    @Select("<script>SELECT permission_id FROM role_permissions WHERE role_id IN <foreach item='id' collection='roleIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Long> selectPermIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
