package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.repository.CategoryRepository;
import com.picmgmt.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ImageMapper imageMapper;
    private final CategoryRepository categoryRepository;

    @Override
    public Category create(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能为空");
        }
        if (categoryName.length() > 20) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_TOO_LONG);
        }

        long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getUserId, userId)
                .eq(Category::getCategoryName, categoryName);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setCategoryName(categoryName);
        categoryRepository.insert(category);

        return category;
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        long userId = StpUtil.getLoginIdAsLong();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!StpUtil.hasRole("admin") && !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // Set images under this category to null (uncategorized)
        LambdaUpdateWrapper<Image> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Image::getCategoryId, categoryId)
                .set(Image::getCategoryId, null);
        imageMapper.update(null, updateWrapper);

        categoryRepository.deleteById(categoryId);
    }

    @Override
    public Category update(Long categoryId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能为空");
        }
        if (categoryName.length() > 20) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_TOO_LONG);
        }

        long userId = StpUtil.getLoginIdAsLong();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!StpUtil.hasRole("admin") && !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        category.setCategoryName(categoryName);
        categoryRepository.updateById(category);

        return category;
    }

    @Override
    public List<Category> listByUser() {
        long userId = StpUtil.getLoginIdAsLong();
        return categoryRepository.listByUserId(userId);
    }
}
