package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picmgmt.entity.CommentLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT * FROM comment_likes WHERE comment_id = #{commentId} AND user_id = #{userId} LIMIT 1")
    CommentLike findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Delete("DELETE FROM comment_likes WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM comment_likes WHERE comment_id = #{commentId}")
    Long countByCommentId(@Param("commentId") Long commentId);

    @Select("SELECT COUNT(*) FROM comment_likes WHERE comment_id = #{commentId} AND user_id = #{userId}")
    Long countByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Delete("DELETE FROM comment_likes WHERE comment_id = #{commentId}")
    int deleteByCommentId(@Param("commentId") Long commentId);
}
