package com.picmgmt.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMapperSqlTest {

    @Test
    void imageNameKeyword_shouldUseCaseInsensitiveContainsMatchForPagedQueries() throws NoSuchMethodException {
        assertUsesCaseInsensitiveContainsMatch(ImageMapper.class.getMethod(
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
        ));
    }

    @Test
    void imageNameKeyword_shouldUseCaseInsensitiveContainsMatchForListQueries() throws NoSuchMethodException {
        assertUsesCaseInsensitiveContainsMatch(ImageMapper.class.getMethod(
                "selectImageVOList",
                Long.class,
                String.class,
                Long.class,
                String.class,
                java.util.List.class,
                String.class,
                String.class,
                String.class,
                String.class
        ));
    }

    private void assertUsesCaseInsensitiveContainsMatch(Method method) {
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertTrue(sql.contains("LOWER(i.image_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')"));
        assertFalse(sql.contains("BINARY i.image_name"));
        assertFalse(sql.contains("i.image_name = #{keyword}"));
    }
}
