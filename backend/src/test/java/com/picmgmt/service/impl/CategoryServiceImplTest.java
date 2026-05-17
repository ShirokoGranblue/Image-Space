package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.common.BusinessException;
import com.picmgmt.entity.Category;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryMapper categoryMapper;
    @Mock private ImageMapper imageMapper;
    @Mock private CategoryRepository categoryRepository;

    private CategoryServiceImpl service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryMapper, imageMapper, categoryRepository);
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void create_shouldThrowForNullName() {
        try {
            service.create(null);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(400, e.getErrorCode().getCode());
        }
    }

    @Test
    void create_shouldThrowForBlankName() {
        try {
            service.create("   ");
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(400, e.getErrorCode().getCode());
        }
    }

    @Test
    void create_shouldThrowForTooLongName() {
        try {
            service.create("a".repeat(21));
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(3003, e.getErrorCode().getCode());
        }
    }

    @Test
    void create_shouldThrowForDuplicateName() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        try {
            service.create("风景");
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(3002, e.getErrorCode().getCode());
        }
    }

    @Test
    void create_shouldSucceedForValidName() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doNothing().when(categoryRepository).insert(any(Category.class));

        Category result = service.create("风景");

        assertNotNull(result);
        assertEquals("风景", result.getCategoryName());
        assertEquals(1L, result.getUserId());
    }

    @Test
    void delete_shouldThrowWhenCategoryNotFound() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        try {
            service.delete(99L);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(3001, e.getErrorCode().getCode());
        }
    }

    @Test
    void delete_shouldThrowWhenNotOwnerAndNotAdmin() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
        stpMock.when(() -> StpUtil.hasRole("admin")).thenReturn(false);

        Category category = new Category();
        category.setUserId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        try {
            service.delete(1L);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(403, e.getErrorCode().getCode());
        }
    }
}
