package com.picmgmt.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.ImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImageRepository {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final StorageService storageService;
    private final CacheService cacheService;

    private static final Duration ENTITY_TTL = Duration.ofMinutes(30);
    private static final Duration PAGE_TTL = Duration.ofMinutes(5);
    private static final String ENTITY_KEY_PREFIX = "image:entity:";
    private static final String PAGE_KEY_PREFIX = "image:page:";

    public Optional<Image> findById(Long id) {
        String key = ENTITY_KEY_PREFIX + id;
        return cacheService.get(key, Image.class)
                .or(() -> {
                    Image image = imageMapper.selectById(id);
                    if (image != null) {
                        cacheService.put(key, image, ENTITY_TTL);
                    }
                    return Optional.ofNullable(image);
                });
    }

    public void insert(Image image) {
        imageMapper.insert(image);
        cacheService.put(ENTITY_KEY_PREFIX + image.getId(), image, ENTITY_TTL);
    }

    public void updateById(Image image) {
        imageMapper.updateById(image);
        cacheService.evict(ENTITY_KEY_PREFIX + image.getId());
        cacheService.evictByPattern(PAGE_KEY_PREFIX + "*");
    }

    public void deleteById(Long id) {
        imageMapper.deleteById(id);
        cacheService.evict(ENTITY_KEY_PREFIX + id);
        cacheService.evictByPattern(PAGE_KEY_PREFIX + "*");
    }

    public ImageVO toVO(Image image) {
        ImageVO vo = new ImageVO();
        vo.setId(image.getId());
        vo.setUserId(image.getUserId());
        vo.setCategoryId(image.getCategoryId());
        vo.setImageName(image.getImageName());
        vo.setStorageKey(image.getStorageKey());
        vo.setFileSize(image.getFileSize());
        vo.setImageType(image.getImageType());
        vo.setDescription(image.getDescription());
        vo.setTags(image.getTags());
        vo.setVisibility(image.getVisibility());
        vo.setVisibleUsernames(image.getVisibleUsernames());
        vo.setUploadTime(image.getUploadTime());

        if (image.getStorageKey() != null) {
            vo.setImageUrl(MediaUrlUtil.imageDownloadUrl(image.getId(), image.getStorageKey()));
        }
        User user = userMapper.selectById(image.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setDisplayName(user.getDisplayName());
        }
        if (image.getCategoryId() != null) {
            Category category = categoryMapper.selectById(image.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        return vo;
    }
}
