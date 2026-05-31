package com.picmgmt.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class BloomFilterService {

    private final UserMapper userMapper;
    private final ImageMapper imageMapper;
    private final CategoryMapper categoryMapper;

    private static final int EXPECTED_INSERTIONS = 100_000;
    private static final double FPP = 0.01;

    private final Object userFilterLock = new Object();
    private final Object imageFilterLock = new Object();
    private final Object categoryFilterLock = new Object();

    private BloomFilter<String> userFilter;
    private BloomFilter<String> imageFilter;
    private BloomFilter<String> categoryFilter;

    public BloomFilterService(UserMapper userMapper, ImageMapper imageMapper, CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.imageMapper = imageMapper;
        this.categoryMapper = categoryMapper;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Bloom filters...");

        userFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS, FPP);
        List<Long> userIds = userMapper.selectIds();
        for (Long id : userIds) {
            userFilter.put(keyFor("user:entity:", id));
        }
        log.info("User BloomFilter loaded: {} ids", userIds.size());

        imageFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS, FPP);
        List<Long> imageIds = imageMapper.selectIds();
        for (Long id : imageIds) {
            imageFilter.put(keyFor("image:entity:", id));
        }
        log.info("Image BloomFilter loaded: {} ids", imageIds.size());

        categoryFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS, FPP);
        List<Long> categoryIds = categoryMapper.selectIds();
        for (Long id : categoryIds) {
            categoryFilter.put(keyFor("category:entity:", id));
        }
        log.info("Category BloomFilter loaded: {} ids", categoryIds.size());
    }

    public boolean mightContain(String cacheKey) {
        if (cacheKey.startsWith("user:entity:")) {
            return userFilter.mightContain(cacheKey);
        }
        if (cacheKey.startsWith("image:entity:")) {
            return imageFilter.mightContain(cacheKey);
        }
        if (cacheKey.startsWith("category:entity:")) {
            return categoryFilter.mightContain(cacheKey);
        }
        return true;
    }

    public void addUser(Long id) {
        synchronized (userFilterLock) {
            userFilter.put(keyFor("user:entity:", id));
        }
    }

    public void addImage(Long id) {
        synchronized (imageFilterLock) {
            imageFilter.put(keyFor("image:entity:", id));
        }
    }

    public void addCategory(Long id) {
        synchronized (categoryFilterLock) {
            categoryFilter.put(keyFor("category:entity:", id));
        }
    }

    private String keyFor(String prefix, Long id) {
        return prefix + id;
    }
}
