package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picmgmt.entity.Image;
import com.picmgmt.vo.ImageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<Image> {

    @Select("""
        <script>
            SELECT i.*, u.username, u.display_name, c.category_name
            FROM images i
            LEFT JOIN users u ON i.user_id = u.id
            LEFT JOIN categories c ON i.category_id = c.id
            <where>
                <if test='userId != null'>AND i.user_id = #{userId}</if>
                <if test='keyword != null and keyword != \"\"'>AND i.image_name LIKE CONCAT('%', #{keyword}, '%')</if>
                <if test='categoryId != null'>AND i.category_id = #{categoryId}</if>
                <if test='visibility != null and visibility != \"\"'>AND i.visibility = #{visibility}</if>
            </where>
            <choose>
                <when test='sortField == \"image_name\"'>ORDER BY i.image_name ${sortOrder}</when>
                <when test='sortField == \"file_size\"'>ORDER BY i.file_size ${sortOrder}</when>
                <otherwise>ORDER BY i.upload_time ${sortOrder}</otherwise>
            </choose>
        </script>
    """)
    List<ImageVO> selectImageVOList(@Param("userId") Long userId,
                                     @Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     @Param("visibility") String visibility,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);
}
