package com.picmgmt.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMapperSqlTest {

    @Test
    void imageNameKeyword_shouldUseBinaryExactMatchInsteadOfLike() throws NoSuchMethodException {
        Method method = ImageMapper.class.getMethod(
                "selectImageVOPage",
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.class,
                Long.class,
                String.class,
                Long.class,
                String.class,
                java.util.List.class,
                String.class,
                String.class,
                String.class,
                String.class
        );

        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertTrue(sql.contains("BINARY i.image_name = #{keyword}"));
        assertFalse(sql.contains("i.image_name LIKE"));
    }
}
