package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.Image;
import com.picmgmt.vo.ImageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<Image> {

    @Select("SELECT id FROM images")
    List<Long> selectIds();

    @Select("""
        <script>
            SELECT i.*,
                   u.uuid as user_uuid,
                   CASE WHEN u.deleted = 1 THEN '已注销用户' ELSE u.username END as username,
                   CASE WHEN u.deleted = 1 THEN '已注销用户' ELSE u.display_name END as display_name,
                   c.category_name
            FROM images i
            LEFT JOIN users u ON i.user_id = u.id
            LEFT JOIN categories c ON i.category_id = c.id
            <where>
                <if test='userId != null'>AND i.user_id = #{userId}</if>
                <if test='keyword != null and keyword != \"\"'>AND BINARY i.image_name = #{keyword}</if>
                <if test='categoryId != null'>AND i.category_id = #{categoryId}</if>
                <if test='visibility != null and visibility != \"\"'>AND i.visibility = #{visibility}</if>
                <if test='tagFilters != null and tagFilters.size > 0'>
                    AND (
                    <foreach collection='tagFilters' item='tag' separator=' OR '>
                        i.tags LIKE CONCAT('%', #{tag}, '%')
                    </foreach>
                    )
                </if>
            </where>
            <choose>
                <when test='sortMode == \"random\"'>ORDER BY MD5(CONCAT(#{randomSeed}, i.id)), i.id</when>
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
                                     @Param("tagFilters") List<String> tagFilters,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder,
                                     @Param("sortMode") String sortMode,
                                     @Param("randomSeed") String randomSeed);

    @Select("""
        <script>
            SELECT i.id, i.uuid, i.user_id, i.category_id, i.image_name, i.image_path, i.storage_key,
                   i.file_size, i.image_type, i.description, i.tags,
                   i.visibility, i.media_version, i.visible_usernames, i.upload_time,
                   u.uuid as user_uuid,
                   CASE WHEN u.deleted = 1 THEN '已注销用户' ELSE u.username END as username,
                   CASE WHEN u.deleted = 1 THEN '已注销用户' ELSE u.display_name END as display_name,
                   c.category_name
            FROM images i
            LEFT JOIN users u ON i.user_id = u.id
            LEFT JOIN categories c ON i.category_id = c.id
            <where>
                <if test='userId != null'>AND i.user_id = #{userId}</if>
                <if test='keyword != null and keyword != \"\"'>AND BINARY i.image_name = #{keyword}</if>
                <if test='categoryId != null'>AND i.category_id = #{categoryId}</if>
                <if test='visibility != null and visibility != \"\"'>AND i.visibility = #{visibility}</if>
                <if test='tagFilters != null and tagFilters.size > 0'>
                    AND (
                    <foreach collection='tagFilters' item='tag' separator=' OR '>
                        i.tags LIKE CONCAT('%', #{tag}, '%')
                    </foreach>
                    )
                </if>
            </where>
            <choose>
                <when test='sortMode == \"random\"'>ORDER BY MD5(CONCAT(#{randomSeed}, i.id)), i.id</when>
                <when test='sortField == \"image_name\"'>ORDER BY i.image_name ${sortOrder}</when>
                <when test='sortField == \"file_size\"'>ORDER BY i.file_size ${sortOrder}</when>
                <otherwise>ORDER BY i.upload_time ${sortOrder}</otherwise>
            </choose>
        </script>
    """)
    Page<ImageVO> selectImageVOPage(Page<ImageVO> page,
                                     @Param("userId") Long userId,
                                     @Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     @Param("visibility") String visibility,
                                     @Param("tagFilters") List<String> tagFilters,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder,
                                     @Param("sortMode") String sortMode,
                                     @Param("randomSeed") String randomSeed);
}
