package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picmgmt.entity.ImageLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImageLikeMapper extends BaseMapper<ImageLike> {

    @Select("SELECT * FROM image_likes WHERE image_id = #{imageId} AND user_id = #{userId} LIMIT 1")
    ImageLike findByImageIdAndUserId(@Param("imageId") Long imageId, @Param("userId") Long userId);

    @Delete("DELETE FROM image_likes WHERE image_id = #{imageId} AND user_id = #{userId}")
    int deleteByImageIdAndUserId(@Param("imageId") Long imageId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM image_likes WHERE image_id = #{imageId}")
    Long countByImageId(@Param("imageId") Long imageId);

    @Select("SELECT COUNT(*) FROM image_likes WHERE image_id = #{imageId} AND user_id = #{userId}")
    Long countByImageIdAndUserId(@Param("imageId") Long imageId, @Param("userId") Long userId);
}
