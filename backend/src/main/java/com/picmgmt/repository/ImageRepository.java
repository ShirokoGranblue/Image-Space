package com.picmgmt.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.image.ImageUrlService;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
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
    private final BloomFilterService bloomFilterService;
    private final ImageUrlService imageUrlService;

    private static final Duration ENTITY_TTL = Duration.ofMinutes(30);
    private static final String ENTITY_KEY_PREFIX = "image:entity:";

    public Optional<Image> findById(Long id) {
        String key = ENTITY_KEY_PREFIX + id;
        return Optional.ofNullable(cacheService.getOrLoad(key, Image.class,
                () -> imageMapper.selectById(id), ENTITY_TTL));
    }

    public Optional<Image> findByUuid(String uuid) {
        Image image = imageMapper.selectOne(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getUuid, uuid));
        return Optional.ofNullable(image);
    }

    public void insert(Image image) {
        imageMapper.insert(image);
        cacheService.put(ENTITY_KEY_PREFIX + image.getId(), image, ENTITY_TTL);
        bloomFilterService.addImage(image.getId());
    }

    public void updateById(Image image) {
        imageMapper.updateById(image);
        cacheService.evict(ENTITY_KEY_PREFIX + image.getId());
    }

    public void deleteById(Long id) {
        imageMapper.deleteById(id);
        cacheService.evict(ENTITY_KEY_PREFIX + id);
    }

    public ImageVO toVO(Image image) {
        ImageVO vo = new ImageVO();
        vo.setId(image.getId());
        vo.setUuid(image.getUuid());
        vo.setUserId(image.getUserId());
        vo.setCategoryId(image.getCategoryId());
        vo.setImageName(image.getImageName());
        vo.setImagePath(image.getImagePath());
        vo.setStorageKey(image.getStorageKey());
        vo.setOriginalKey(image.getOriginalKey());
        vo.setOriginalFilename(image.getOriginalFilename());
        vo.setOriginalContentType(image.getOriginalContentType());
        vo.setOriginalExt(image.getOriginalExt());
        vo.setOriginalSize(image.getOriginalSize());
        vo.setWidth(image.getWidth());
        vo.setHeight(image.getHeight());
        vo.setMediumKey(image.getMediumKey());
        vo.setThumbKey(image.getThumbKey());
        vo.setFileSize(image.getFileSize());
        vo.setImageType(image.getImageType());
        vo.setDescription(image.getDescription());
        vo.setTags(image.getTags());
        vo.setVisibility(image.getVisibility());
        vo.setMediaVersion(image.getMediaVersion());
        vo.setVisibleUsernames(image.getVisibleUsernames());
        vo.setUploadTime(image.getUploadTime());

        String originalKey = firstNonBlank(image.getOriginalKey(), image.getStorageKey());
        if (originalKey != null && !originalKey.isBlank()) {
            String originalUrl = accessUrlFor(image, originalKey);
            vo.setOriginalUrl(originalUrl);
            vo.setImageUrl(originalUrl);
            if ("PUBLIC".equals(image.getVisibility())) {
                vo.setPublicUrl(originalUrl);
            } else {
                vo.setPrivateUrl(originalUrl);
            }
        } else if (image.getImagePath() != null && image.getImagePath().startsWith("data:image/")) {
            String downloadUrl = "/api/image/download/" + image.getUuid();
            vo.setImageUrl(downloadUrl);
            vo.setOriginalUrl(downloadUrl);
        }
        if (image.getMediumKey() != null && !image.getMediumKey().isBlank()) {
            vo.setMediumUrl(accessUrlFor(image, image.getMediumKey()));
        }
        if (image.getThumbKey() != null && !image.getThumbKey().isBlank()) {
            vo.setThumbUrl(accessUrlFor(image, image.getThumbKey()));
        }
        User user = userMapper.selectById(image.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setDisplayName(user.getDisplayName());
            vo.setUserUuid(user.getUuid());
        }
        if (image.getCategoryId() != null) {
            Category category = categoryMapper.selectById(image.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        return vo;
    }

    private String accessUrlFor(Image image, String key) {
        if ("PUBLIC".equals(image.getVisibility())) {
            return imageUrlService.getPublicImageUrl(key, image.getMediaVersion());
        }
        return imageUrlService.getPrivateImageUrl(key);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
