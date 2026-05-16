package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picmgmt.entity.Comment;
import com.picmgmt.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("""
        SELECT c.id, c.image_id, c.user_id, c.content, c.image_path, c.create_time, u.username, u.display_name
        FROM comments c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.image_id = #{imageId}
        ORDER BY c.create_time DESC
    """)
    List<CommentVO> selectCommentVOList(Long imageId);
}
