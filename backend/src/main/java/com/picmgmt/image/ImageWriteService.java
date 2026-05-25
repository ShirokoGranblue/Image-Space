package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Category;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CategoryMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageWriteService {

    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final CategoryMapper categoryMapper;
    private final ImagePermissionService permissionService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @Transactional
    public ImageVO upload(MultipartFile file, Long categoryId, String description,
                          String tags, String visibility, String visibleUsernames, String imageName) {
        if (file.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名无效");
        }

        String ext = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_INVALID);
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.IMAGE_DESCRIPTION_TOO_LONG);
        }

        long userId = StpUtil.getLoginIdAsLong();

        if (categoryId != null) {
            Category category = categoryMapper.selectById(categoryId);
            if (category == null || !category.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }

        if (!isValidImageContent(bytes)) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_INVALID);
        }

        String objectKey = userId + "/" + UUID.randomUUID() + "." + ext;
        String mimeType = switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
        storageService.upload("images", objectKey, bytes, mimeType);

        Image image = new Image();
        image.setUserId(userId);
        image.setCategoryId(categoryId);
        image.setImageName(buildStoredImageName(originalFilename, imageName, ext));
        image.setStorageKey(objectKey);
        image.setFileSize(file.getSize());
        image.setImageType(ext.toUpperCase());
        image.setDescription(description);
        image.setTags(tags);
        image.setVisibility(visibility != null && visibility.matches("(?i)PUBLIC|PRIVATE|SPECIFIED")
                ? visibility.trim().toUpperCase() : "PRIVATE");
        image.setVisibleUsernames(visibleUsernames);
        image.setUploadTime(LocalDateTime.now());
        imageRepository.insert(image);

        return imageRepository.toVO(image);
    }

    @Transactional
    public void delete(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        permissionService.validateOwnershipOrAdmin(image);

        if (image.getStorageKey() != null) {
            storageService.delete("images", image.getStorageKey());
        }
        imageRepository.deleteById(imageId);
    }

    @Transactional
    public ImageVO update(Long imageId, ImageUpdateDTO dto) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        permissionService.validateOwnershipOrAdmin(image);

        long userId = StpUtil.getLoginIdAsLong();

        if (dto.getImageName() != null) image.setImageName(dto.getImageName());
        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null || !category.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            image.setCategoryId(dto.getCategoryId());
        }
        if (dto.getDescription() != null) {
            if (dto.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new BusinessException(ErrorCode.IMAGE_DESCRIPTION_TOO_LONG);
            }
            image.setDescription(dto.getDescription());
        }
        if (dto.getTags() != null) image.setTags(dto.getTags());
        if (dto.getVisibility() != null) image.setVisibility(dto.getVisibility().trim().toUpperCase());
        if (dto.getVisibleUsernames() != null) image.setVisibleUsernames(dto.getVisibleUsernames());

        imageRepository.updateById(image);
        return imageRepository.toVO(image);
    }

    private boolean isValidImageContent(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return true;
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return true;
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return true;
        return false;
    }

    private String buildStoredImageName(String originalFilename, String imageName, String ext) {
        String fallbackBody = FileUtil.mainName(originalFilename);
        String body = imageName == null || imageName.isBlank() ? fallbackBody : imageName.trim();
        body = FileUtil.mainName(body);
        body = body.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (body.isBlank()) {
            body = fallbackBody;
        }
        return body + "." + ext;
    }
}
