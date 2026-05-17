package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.ImageCacheService;
import com.picmgmt.service.ImageService;
import com.picmgmt.vo.ImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_VISIBILITY = Set.of("PRIVATE", "PUBLIC", "SPECIFIED");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final ImageCacheService imageCacheService;

    @Override
    @Transactional
    public ImageVO upload(MultipartFile file, Long categoryId, String description, String tags,
                          String visibility, String visibleUsernames) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名无效");
        }
        String ext = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、JPEG、WEBP 格式");
        }

        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("描述长度不能超过" + MAX_DESCRIPTION_LENGTH + "个字符");
        }

        long userId = StpUtil.getLoginIdAsLong();

        // Validate category ownership
        if (categoryId != null) {
            Category category = categoryMapper.selectById(categoryId);
            if (category == null || !category.getUserId().equals(userId)) {
                throw new IllegalArgumentException("分类不存在");
            }
        }

        // 读取文件字节到内存缓冲区
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }

        // 在内存中校验 magic bytes
        if (!isValidImageContent(bytes)) {
            throw new IllegalArgumentException("文件类型校验失败，仅支持真实的 JPG、PNG、WEBP 图片");
        }

        // 将字节数组编码为 Base64 Data URL，同时写入缓冲区
        String mimeType = ImageCacheService.getMimeType(ext);
        // 先创建临时缓存键，insert 后再用真实 ID 更新
        String dataUrl = imageCacheService.encodeToDataUrl("image:temp", bytes, mimeType);

        Image image = new Image();
        image.setUserId(userId);
        image.setCategoryId(categoryId);
        image.setImageName(originalFilename);
        image.setImagePath(dataUrl);
        image.setFileSize(file.getSize());
        image.setImageType(ext.toUpperCase());
        image.setDescription(description);
        image.setTags(tags);
        image.setVisibility(normalizeVisibility(visibility));
        image.setVisibleUsernames(normalizeVisibleUsernames(image.getVisibility(), visibleUsernames));
        image.setUploadTime(LocalDateTime.now());
        imageMapper.insert(image);

        // 用真实 ID 重新缓存，移除临时键
        imageCacheService.evict("image:temp");
        imageCacheService.encodeToDataUrl("image:" + image.getId(), bytes, mimeType);

        return toVO(image);
    }

    @Override
    @Transactional
    public void delete(Long imageId) {
        long userId = StpUtil.getLoginIdAsLong();
        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        if (!StpUtil.hasRole("admin") && !image.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该图片");
        }

        // 删除数据库记录
        imageMapper.deleteById(imageId);

        // 移除缓存
        imageCacheService.evict("image:" + imageId);
    }

    @Override
    public ImageVO update(Long imageId, String imageName, Long categoryId, String description, String tags,
                          String visibility, String visibleUsernames) {
        long userId = StpUtil.getLoginIdAsLong();
        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        if (!StpUtil.hasRole("admin") && !image.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改该图片");
        }

        if (imageName != null) {
            image.setImageName(imageName);
        }
        if (categoryId != null) {
            Category category = categoryMapper.selectById(categoryId);
            if (category == null || !category.getUserId().equals(userId)) {
                throw new IllegalArgumentException("分类不存在");
            }
            image.setCategoryId(categoryId);
        }
        if (description != null) {
            if (description.length() > MAX_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("描述长度不能超过" + MAX_DESCRIPTION_LENGTH + "个字符");
            }
            image.setDescription(description);
        }
        if (tags != null) {
            image.setTags(tags);
        }
        boolean visibilityChanged = visibility != null;
        if (visibilityChanged) {
            image.setVisibility(normalizeVisibility(visibility));
        }
        if (visibilityChanged || visibleUsernames != null) {
            image.setVisibleUsernames(normalizeVisibleUsernames(image.getVisibility(), visibleUsernames));
        }
        imageMapper.updateById(image);

        return toVO(image);
    }

    @Override
    public Page<ImageVO> page(ImageQueryDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();

        // Validate sortField to prevent SQL injection
        Set<String> allowedFields = Set.of("upload_time", "image_name", "file_size");
        String sortField = allowedFields.contains(dto.getSortField()) ? dto.getSortField() : "upload_time";
        String sortOrder = "asc".equalsIgnoreCase(dto.getSortOrder()) ? "asc" : "desc";

        List<ImageVO> records = imageMapper.selectImageVOList(
                userId, dto.getKeyword(), dto.getCategoryId(), null, sortField, sortOrder);

        int page = dto.getPage();
        int limit = dto.getLimit();
        int total = records.size();
        int fromIndex = (page - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, total);

        Page<ImageVO> result = new Page<>(page, limit, total);
        result.setRecords(fromIndex >= total ? List.of() : records.subList(fromIndex, toIndex));
        return result;
    }

    @Override
    public ImageVO getById(Long imageId) {
        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        if (!canView(image)) {
            throw new IllegalArgumentException("无权查看该图片");
        }
        return toVO(image);
    }

    @Override
    public Page<ImageVO> getSquare(Integer page, Integer limit) {
        List<ImageVO> records = imageMapper.selectImageVOList(
                null, null, null, "PUBLIC", "upload_time", "desc");

        int total = records.size();
        int fromIndex = (page - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, total);

        Page<ImageVO> result = new Page<>(page, limit, total);
        result.setRecords(fromIndex >= total ? List.of() : records.subList(fromIndex, toIndex));
        return result;
    }

    private ImageVO toVO(Image image) {
        ImageVO vo = BeanUtil.copyProperties(image, ImageVO.class);
        vo.setUsername("");

        User user = userMapper.selectById(image.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }
        if (image.getCategoryId() != null) {
            Category category = categoryMapper.selectById(image.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        return vo;
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return "PRIVATE";
        }
        String normalized = visibility.trim().toUpperCase();
        if (!ALLOWED_VISIBILITY.contains(normalized)) {
            throw new IllegalArgumentException("可见权限无效");
        }
        return normalized;
    }

    private String normalizeVisibleUsernames(String visibility, String visibleUsernames) {
        if (!"SPECIFIED".equals(visibility)) {
            return null;
        }
        Set<String> usernames = Arrays.stream((visibleUsernames == null ? "" : visibleUsernames).split("[,，\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (usernames.isEmpty()) {
            throw new IllegalArgumentException("指定用户权限至少需要填写一个用户名");
        }
        for (String username : usernames) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            if (userMapper.selectCount(wrapper) == 0) {
                throw new IllegalArgumentException("指定用户不存在: " + username);
            }
        }
        return String.join(",", usernames);
    }

    private boolean canView(Image image) {
        if ("PUBLIC".equals(image.getVisibility())) {
            return true;
        }
        if (!StpUtil.isLogin()) {
            return false;
        }
        long userId = StpUtil.getLoginIdAsLong();
        if (image.getUserId().equals(userId) || StpUtil.hasRole("admin")) {
            return true;
        }
        if (!"SPECIFIED".equals(image.getVisibility())) {
            return false;
        }
        User viewer = userMapper.selectById(userId);
        if (viewer == null || image.getVisibleUsernames() == null) {
            return false;
        }
        return Arrays.stream(image.getVisibleUsernames().split(","))
                .map(String::trim)
                .anyMatch(viewer.getUsername()::equals);
    }

    /**
     * 在内存中校验图片 magic bytes（不再写入文件）
     */
    private boolean isValidImageContent(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;

        // PNG: 89 50 4E 47
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }
        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return true;
        }
        // WEBP: 52 49 46 46 ... 57 45 42 50
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) {
            return true;
        }
        return false;
    }
}
