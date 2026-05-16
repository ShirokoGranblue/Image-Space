package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
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

    @Override
    public Category create(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (categoryName.length() > 20) {
            throw new IllegalArgumentException("分类名称长度不能超过20个字符");
        }

        long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getUserId, userId)
                .eq(Category::getCategoryName, categoryName);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setCategoryName(categoryName);
        categoryMapper.insert(category);

        return category;
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        long userId = StpUtil.getLoginIdAsLong();
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        if (!StpUtil.hasRole("admin") && !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该分类");
        }

        // Set images under this category to null (未分类)
        LambdaUpdateWrapper<Image> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Image::getCategoryId, categoryId)
                .set(Image::getCategoryId, null);
        imageMapper.update(null, updateWrapper);

        categoryMapper.deleteById(categoryId);
    }

    @Override
    public Category update(Long categoryId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (categoryName.length() > 20) {
            throw new IllegalArgumentException("分类名称长度不能超过20个字符");
        }

        long userId = StpUtil.getLoginIdAsLong();
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        if (!StpUtil.hasRole("admin") && !category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改该分类");
        }

        category.setCategoryName(categoryName);
        categoryMapper.updateById(category);

        return category;
    }

    @Override
    public List<Category> listByUser() {
        long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getUserId, userId)
                .orderByAsc(Category::getId);
        return categoryMapper.selectList(wrapper);
    }
}
