package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picmgmt.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id FROM users")
    List<Long> selectIds();
}
