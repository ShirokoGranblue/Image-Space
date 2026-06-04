package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picmgmt.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
