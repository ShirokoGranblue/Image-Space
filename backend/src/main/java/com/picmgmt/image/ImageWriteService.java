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
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final String SPECIFIED_USERS_REQUIRED_MESSAGE =
            "SPECIFIED visibility requires at least one username";

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

        String mimeType = ImageConvertUtil.contentTypeForExt(ext);

        String resolvedVisibility = resolveVisibility(visibility);
        String normalizedVisibleUsernames = normalizeVisibleUsernames(visibleUsernames);
        validateSpecifiedUsers(resolvedVisibility, normalizedVisibleUsernames);
        String cacheControl = ImageUrlService.cacheControlForVisibility(resolvedVisibility);

        String imageUuid = UUID.randomUUID().toString();
        String objectPrefix = "images/" + imageUuid;
        String originalKey = objectPrefix + "/original." + ext;
        ImageVariant medium = null;
        ImageVariant thumb = null;
        Integer originalWidth = null;
        Integer originalHeight = null;
        if (!ImageConvertUtil.isWebp(mimeType, ext, bytes)) {
            BufferedImage originalImage = ImageConvertUtil.readSupportedImage(bytes, mimeType, ext);
            originalWidth = originalImage.getWidth();
            originalHeight = originalImage.getHeight();
            medium = ImageConvertUtil.createDisplayVariant(bytes, mimeType, ext, 1200);
            thumb = ImageConvertUtil.createDisplayVariant(bytes, mimeType, ext, 400);
        }

        List<String> uploadedKeys = new ArrayList<>();
        try {
            storageService.upload("images", originalKey, bytes, mimeType, cacheControl);
            uploadedKeys.add(originalKey);

            String mediumKey = null;
            if (medium != null) {
                mediumKey = objectPrefix + "/medium." + medium.ext();
                storageService.upload("images", mediumKey, medium.bytes(), medium.contentType(), cacheControl);
                uploadedKeys.add(mediumKey);
            }

            String thumbKey = null;
            if (thumb != null) {
                thumbKey = objectPrefix + "/thumb." + thumb.ext();
                storageService.upload("images", thumbKey, thumb.bytes(), thumb.contentType(), cacheControl);
                uploadedKeys.add(thumbKey);
            }

            Image image = new Image();
            image.setUuid(imageUuid);
            image.setUserId(userId);
            image.setCategoryId(categoryId);
            image.setImageName(buildStoredImageName(originalFilename, imageName, ext));
            image.setStorageKey(originalKey);
            image.setOriginalKey(originalKey);
            image.setOriginalFilename(originalFilename);
            image.setOriginalContentType(mimeType);
            image.setOriginalExt(normalizeExt(ext));
            image.setOriginalSize(file.getSize());
            image.setWidth(originalWidth);
            image.setHeight(originalHeight);
            image.setMediumKey(mediumKey);
            image.setThumbKey(thumbKey);
            image.setFileSize(file.getSize());
            image.setImageType(ext.toUpperCase());
            image.setDescription(description);
            image.setTags(tags);
            image.setVisibility(resolvedVisibility);
            image.setMediaVersion(1L);
            image.setVisibleUsernames("SPECIFIED".equals(resolvedVisibility) ? normalizedVisibleUsernames : null);
            image.setUploadTime(LocalDateTime.now());
            imageRepository.insert(image);

            return imageRepository.toVO(image);
        } catch (RuntimeException e) {
            cleanupUploadedObjects(uploadedKeys);
            throw e;
        }
    }

    @Transactional
    public void delete(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        permissionService.validateOwnershipOrAdmin(image);

        deleteImageObjects(image);
        imageRepository.deleteById(imageId);
        evictMediaState(image);
        purgePublicUrlIfNeeded(image);
    }

    @Transactional
    public void deleteByUuid(String uuid) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        permissionService.validateOwnershipOrAdmin(image);
        deleteImageObjects(image);
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
        List<String> oldPublicUrls = "PUBLIC".equals(oldVisibility)
                ? mediaKeys(image).stream()
                .map(key -> imageUrlService.getPublicImageUrl(key, oldMediaVersion))
                .filter(url -> url != null && !url.isBlank())
                .toList()
                : List.of();
        boolean visibilityChanged = false;
        boolean publicnessChanged = false;
        String newVisibility = image.getVisibility();
        String providedVisibleUsernames = dto.getVisibleUsernames() == null
                ? ""
                : normalizeVisibleUsernames(dto.getVisibleUsernames());
        String normalizedVisibleUsernames = dto.getVisibleUsernames() == null
                ? image.getVisibleUsernames()
                : providedVisibleUsernames;

        if (dto.getVisibility() != null) {
            newVisibility = resolveVisibility(dto.getVisibility());
            if ("SPECIFIED".equals(newVisibility) && !"SPECIFIED".equals(image.getVisibility())) {
                validateSpecifiedUsers(newVisibility, providedVisibleUsernames);
            }
            if (!newVisibility.equals(image.getVisibility())) {
                visibilityChanged = true;
                publicnessChanged = "PUBLIC".equals(oldVisibility) != "PUBLIC".equals(newVisibility);
                if (!"PUBLIC".equals(oldVisibility) && "PUBLIC".equals(newVisibility)) {
                    image.setMediaVersion(oldMediaVersion + 1);
                }
            }
            image.setVisibility(newVisibility);
        }
        validateSpecifiedUsers(newVisibility, normalizedVisibleUsernames);

        String finalVisibleUsernames = "SPECIFIED".equals(newVisibility)
                ? normalizedVisibleUsernames
                : null;
        boolean visibleUsersChanged = !Objects.equals(finalVisibleUsernames, image.getVisibleUsernames());
        if (dto.getVisibleUsernames() != null || !"SPECIFIED".equals(newVisibility)) {
            image.setVisibleUsernames(finalVisibleUsernames);
        }

        image.setUploadTime(LocalDateTime.now());

        imageRepository.updateById(image);
        if (visibilityChanged || visibleUsersChanged) {
            for (String key : mediaKeys(image)) {
                imageUrlService.evictPrivateAccess(key);
            }
        }
        if (visibilityChanged) {
            evictMediaMeta(image);
        }
        if (publicnessChanged) {
            boolean isPublic = "PUBLIC".equals(image.getVisibility());
            for (String key : mediaKeys(image)) {
                storageService.updateObjectMetadata("images", key,
                        ImageUrlService.cacheControlForVisibility(image.getVisibility()), isPublic);
            }
            if (!isPublic) {
                oldPublicUrls.forEach(cloudflareCachePurgeService::purgeFile);
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

    private String normalizeVisibleUsernames(String visibleUsernames) {
        if (visibleUsernames == null) {
            return "";
        }
        return Arrays.stream(visibleUsernames.split("[,\\s]+"))
                .map(String::trim)
                .filter(username -> !username.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private void validateSpecifiedUsers(String visibility, String visibleUsernames) {
        if ("SPECIFIED".equals(visibility) && (visibleUsernames == null || visibleUsernames.isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, SPECIFIED_USERS_REQUIRED_MESSAGE);
        }
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
        if (image == null) return;
        for (String key : mediaKeys(image)) {
            imageUrlService.evictPrivateAccess(key);
            mediaMetaCacheService.evict(key);
        }
    }

    private void purgePublicUrlIfNeeded(Image image) {
        if (image == null || !"PUBLIC".equals(image.getVisibility())) {
            return;
        }
        mediaKeys(image).stream()
                .map(key -> imageUrlService.getPublicImageUrl(key, normalizeMediaVersion(image.getMediaVersion())))
                .filter(url -> url != null && !url.isBlank())
                .forEach(cloudflareCachePurgeService::purgeFile);
    }

    private Long normalizeMediaVersion(Long mediaVersion) {
        return mediaVersion == null || mediaVersion < 1 ? 1L : mediaVersion;
    }

    private void cleanupUploadedObjects(List<String> uploadedKeys) {
        for (String key : uploadedKeys) {
            storageService.deleteObjectIfExists("images", key);
        }
    }

    private void deleteImageObjects(Image image) {
        for (String key : mediaKeys(image)) {
            storageService.deleteObjectIfExists("images", key);
        }
        for (String key : downloadCacheKeys(image)) {
            storageService.deleteObjectIfExists("images", key);
        }
    }

    private void evictMediaMeta(Image image) {
        for (String key : mediaKeys(image)) {
            mediaMetaCacheService.evict(key);
        }
    }

    private List<String> mediaKeys(Image image) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        addKey(keys, image.getStorageKey());
        addKey(keys, image.getOriginalKey());
        addKey(keys, image.getMediumKey());
        addKey(keys, image.getThumbKey());
        return new ArrayList<>(keys);
    }

    private List<String> downloadCacheKeys(Image image) {
        String root = downloadCacheRoot(image);
        if (root == null || root.isBlank()) {
            return List.of();
        }
        return List.of(
                root + "/original.jpg",
                root + "/original.png",
                root + "/original.gif"
        );
    }

    private String downloadCacheRoot(Image image) {
        String key = firstNonBlank(image.getOriginalKey(), image.getStorageKey());
        if (key != null && key.contains("/original.")) {
            return key.substring(0, key.lastIndexOf("/original.")) + "/download";
        }
        if (image.getUuid() != null && !image.getUuid().isBlank()) {
            return "images/" + image.getUuid() + "/download";
        }
        return null;
    }

    private void addKey(Set<String> keys, String key) {
        if (key != null && !key.isBlank()) {
            keys.add(key);
        }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String normalizeExt(String ext) {
        return "jpeg".equalsIgnoreCase(ext) ? "jpg" : ext.toLowerCase();
    }
}
