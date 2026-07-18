package com.picmgmt.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageLikeMapperSqlTest {

    @Test
    void publicLikeAggregateUsesAllExistingPublicImagesOwnedByTheProfileUser() throws Exception {
        Method method = ImageLikeMapper.class.getMethod("countPublicLikesByOwnerId", Long.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertTrue(sql.contains("INNER JOIN images i ON i.id = il.image_id"));
        assertTrue(sql.contains("i.user_id = #{ownerUserId}"));
        assertTrue(sql.contains("i.visibility = 'PUBLIC'"));
        assertFalse(sql.contains("PRIVATE"));
        assertFalse(sql.contains("SPECIFIED"));
        assertFalse(sql.toUpperCase().contains("LIMIT"));
    }
}
