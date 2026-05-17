package com.picmgmt.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Category;
import com.picmgmt.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final CategoryMapper categoryMapper;
    private final CacheService cacheService;

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "category:entity:";
    private static final String LIST_KEY_PREFIX = "category:list:";

    public Optional<Category> findById(Long id) {
        String key = KEY_PREFIX + id;
        return cacheService.get(key, Category.class)
                .or(() -> {
                    Category category = categoryMapper.selectById(id);
                    if (category != null) cacheService.put(key, category, TTL);
                    return Optional.ofNullable(category);
                });
    }

    public void insert(Category category) {
        categoryMapper.insert(category);
        cacheService.evictByPattern(LIST_KEY_PREFIX + category.getUserId() + "*");
    }

    public void updateById(Category category) {
        categoryMapper.updateById(category);
        cacheService.evict(KEY_PREFIX + category.getId());
        cacheService.evictByPattern(LIST_KEY_PREFIX + category.getUserId() + "*");
    }

    public void deleteById(Long id) {
        Category category = categoryMapper.selectById(id);
        categoryMapper.deleteById(id);
        if (category != null) {
            cacheService.evict(KEY_PREFIX + id);
            cacheService.evictByPattern(LIST_KEY_PREFIX + category.getUserId() + "*");
        }
    }

    public List<Category> listByUserId(Long userId) {
        String key = LIST_KEY_PREFIX + userId;
        return cacheService.getOrLoad(key, (Class<List<Category>>)(Class<?>)List.class,
                () -> {
                    LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Category::getUserId, userId).orderByAsc(Category::getId);
                    return categoryMapper.selectList(wrapper);
                }, TTL);
    }
}
