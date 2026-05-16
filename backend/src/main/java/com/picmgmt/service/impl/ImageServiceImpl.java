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
import com.picmgmt.service.ImageService;
import com.picmgmt.vo.ImageVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_VISIBILITY = Set.of("PRIVATE", "PUBLIC", "SPECIFIED");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    @Value("${app.upload-path:./upload}")
    private String uploadPath;

    private Path uploadBasePath;

    @PostConstruct
    public void init() {
        this.uploadBasePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.uploadBasePath.toFile().mkdirs();
    }

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

        // Build path: upload/2026/05/uuid.ext
        LocalDate now = LocalDate.now();
        String relativeDir = now.getYear() + "/" + String.format("%02d", now.getMonthValue());
        String fileName = UUID.randomUUID() + "." + ext;

        File dir = uploadBasePath.resolve(relativeDir).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, fileName);

        // Save file first, then validate magic bytes
        try {
            file.transferTo(dest);
            if (!isValidImageContent(dest)) {
                FileUtil.del(dest);
                throw new IllegalArgumentException("文件类型校验失败，仅支持真实的 JPG、PNG、WEBP 图片");
            }
        } catch (IOException e) {
            FileUtil.del(dest);
            throw new RuntimeException("文件保存失败", e);
        }

        Image image = new Image();
        image.setUserId(userId);
        image.setCategoryId(categoryId);
        image.setImageName(originalFilename);
        image.setImagePath("/upload/" + relativeDir + "/" + fileName);
        image.setFileSize(file.getSize());
        image.setImageType(ext.toUpperCase());
        image.setDescription(description);
        image.setTags(tags);
        image.setVisibility(normalizeVisibility(visibility));
        image.setVisibleUsernames(normalizeVisibleUsernames(image.getVisibility(), visibleUsernames));
        image.setUploadTime(LocalDateTime.now());
        imageMapper.insert(image);

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

        // Delete DB record first, then file
        imageMapper.deleteById(imageId);

        String relativePath = image.getImagePath().replace("/upload", "");
        Path filePath = uploadBasePath.resolve(relativePath).normalize();
        if (filePath.startsWith(uploadBasePath)) {
            FileUtil.del(filePath.toFile());
        }
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

    private boolean isValidImageContent(File file) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (FileInputStream fis = new FileInputStream(file)) {
            read = fis.read(header);
        }
        if (read < 4) return false;

        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return true;
        }
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return true;
        }
        // WEBP: 52 49 46 46 ... 57 45 42 50
        if (read >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return true;
        }
        return false;
    }
}
