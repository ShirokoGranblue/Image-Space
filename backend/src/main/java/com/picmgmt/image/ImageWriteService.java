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
    private final ImageUrlService imageUrlService;
    private final MediaMetaCacheService mediaMetaCacheService;
    private final CloudflareCachePurgeService cloudflareCachePurgeService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @Transactional
    public ImageVO upload(MultipartFile file, Long categoryId, String description,
                          String tags, String visibility, String visibleUsernames, String imageName) {
        if (file.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "File cannot be empty");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file name");
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

        String objectKey = "images/" + UUID.randomUUID() + "." + ext;
        String mimeType = switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };

        String resolvedVisibility = resolveVisibility(visibility);
        String cacheControl = ImageUrlService.cacheControlForVisibility(resolvedVisibility);
        storageService.upload("images", objectKey, bytes, mimeType, cacheControl);

        Image image = new Image();
        image.setUuid(java.util.UUID.randomUUID().toString());
        image.setUserId(userId);
        image.setCategoryId(categoryId);
        image.setImageName(buildStoredImageName(originalFilename, imageName, ext));
        image.setStorageKey(objectKey);
        image.setFileSize(file.getSize());
        image.setImageType(ext.toUpperCase());
        image.setDescription(description);
        image.setTags(tags);
        image.setVisibility(resolvedVisibility);
        image.setMediaVersion(1L);
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
        evictMediaState(image);
        purgePublicUrlIfNeeded(image);
    }

    @Transactional
    public void deleteByUuid(String uuid) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        permissionService.validateOwnershipOrAdmin(image);
        if (image.getStorageKey() != null) {
            storageService.delete("images", image.getStorageKey());
        }
        imageRepository.deleteById(image.getId());
        evictMediaState(image);
        purgePublicUrlIfNeeded(image);
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

        String oldVisibility = image.getVisibility();
        Long oldMediaVersion = normalizeMediaVersion(image.getMediaVersion());
        String oldPublicUrl = "PUBLIC".equals(oldVisibility)
                ? imageUrlService.getPublicImageUrl(image.getStorageKey(), oldMediaVersion)
                : null;
        boolean visibilityChanged = false;
        boolean publicnessChanged = false;

        if (dto.getVisibility() != null) {
            String newVisibility = resolveVisibility(dto.getVisibility());
            if (!newVisibility.equals(image.getVisibility())) {
                visibilityChanged = true;
                publicnessChanged = "PUBLIC".equals(oldVisibility) != "PUBLIC".equals(newVisibility);
                if (!"PUBLIC".equals(oldVisibility) && "PUBLIC".equals(newVisibility)) {
                    image.setMediaVersion(oldMediaVersion + 1);
                }
            }
            image.setVisibility(newVisibility);
        }

        boolean visibleUsersChanged = false;
        if (dto.getVisibleUsernames() != null) {
            visibleUsersChanged = !dto.getVisibleUsernames().equals(image.getVisibleUsernames());
            image.setVisibleUsernames(dto.getVisibleUsernames());
        }

        image.setUploadTime(LocalDateTime.now());

        imageRepository.updateById(image);
        if (visibilityChanged || visibleUsersChanged) {
            imageUrlService.evictPrivateAccess(image.getStorageKey());
        }
        if (visibilityChanged) {
            mediaMetaCacheService.evict(image.getStorageKey());
        }
        if (publicnessChanged && image.getStorageKey() != null && !image.getStorageKey().isBlank()) {
            boolean isPublic = "PUBLIC".equals(image.getVisibility());
            storageService.updateObjectMetadata("images", image.getStorageKey(),
                    ImageUrlService.cacheControlForVisibility(image.getVisibility()), isPublic);
            if (!isPublic && oldPublicUrl != null) {
                cloudflareCachePurgeService.purgeFile(oldPublicUrl);
            }
        }
        return imageRepository.toVO(image);
    }

    @Transactional
    public ImageVO updateByUuid(String uuid, ImageUpdateDTO dto) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        return update(image.getId(), dto);
    }

    private boolean isValidImageContent(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return true;
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return true;
        if (bytes.length >= 6 && bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46
                && bytes[3] == 0x38 && (bytes[4] == 0x37 || bytes[4] == 0x39) && bytes[5] == 0x61) return true;
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return true;
        return false;
    }

    private String resolveVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return "PUBLIC";
        }
        String normalized = visibility.trim().toUpperCase();
        if (!normalized.matches("PUBLIC|PRIVATE|SPECIFIED")) {
            throw new BusinessException(ErrorCode.IMAGE_VISIBILITY_INVALID);
        }
        return normalized;
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

    private void evictMediaState(Image image) {
        if (image == null || image.getStorageKey() == null || image.getStorageKey().isBlank()) {
            return;
        }
        imageUrlService.evictPrivateAccess(image.getStorageKey());
        mediaMetaCacheService.evict(image.getStorageKey());
    }

    private void purgePublicUrlIfNeeded(Image image) {
        if (image == null || image.getStorageKey() == null || image.getStorageKey().isBlank()
                || !"PUBLIC".equals(image.getVisibility())) {
            return;
        }
        cloudflareCachePurgeService.purgeFile(
                imageUrlService.getPublicImageUrl(image.getStorageKey(), normalizeMediaVersion(image.getMediaVersion()))
        );
    }

    private Long normalizeMediaVersion(Long mediaVersion) {
        return mediaVersion == null || mediaVersion < 1 ? 1L : mediaVersion;
    }
}
