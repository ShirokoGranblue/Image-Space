package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageReadService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final ImageLikeMapper imageLikeMapper;
    private final UserMapper userMapper;
    private final ImagePermissionService permissionService;
    private final StorageService storageService;
    private final ImageUrlService imageUrlService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("upload_time", "image_name", "file_size");
    private static final Set<String> ALLOWED_SQUARE_SORT_FIELDS = Set.of("upload_time", "file_size", "image_name");
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(30, 50, 100);

    public ImageVO getById(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        ImageVO vo = imageRepository.toVO(image);
        decorateUrls(vo);
        decorateViewerInfo(vo);
        return vo;
    }

    public ImageVO getByUuid(String uuid) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        ImageVO vo = imageRepository.toVO(image);
        decorateUrls(vo);
        decorateViewerInfo(vo);
        return vo;
    }

    public Page<ImageVO> page(ImageQueryDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        String sortField = ALLOWED_SORT_FIELDS.contains(dto.getSortField()) ? dto.getSortField() : "upload_time";
        String sortOrder = "asc".equalsIgnoreCase(dto.getSortOrder()) ? "asc" : "desc";

        Page<ImageVO> pageParam = new Page<>(dto.getPage(), normalizeLimit(dto.getLimit()));
        Page<ImageVO> result = imageMapper.selectImageVOPage(
                pageParam, userId, normalizeKeyword(dto.getKeyword()), dto.getCategoryId(), null, null, sortField, sortOrder, "latest", null);
        decorateUrls(result.getRecords());
        decorateViewerInfoBatch(result.getRecords(), false);
        return result;
    }

    public Page<ImageVO> getSquare(Integer page, Integer limit, String keyword, String tags, String sortMode,
                                   String randomSeed, String sortField, String sortOrder) {
        Page<ImageVO> pageParam = new Page<>(page, normalizeLimit(limit));
        List<String> tagFilters = parseTags(tags);
        String safeSortMode = "latest".equalsIgnoreCase(sortMode) ? "latest" : "random";
        String safeSortField = sortField != null && ALLOWED_SQUARE_SORT_FIELDS.contains(sortField) ? sortField : "upload_time";
        String safeSortOrder = "image_name".equals(safeSortField) || "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
        String safeRandomSeed = "random".equals(safeSortMode)
                ? (randomSeed == null || randomSeed.isBlank() ? "square" : randomSeed.trim())
                : null;
        Page<ImageVO> result = imageMapper.selectImageVOPage(
                pageParam, null, normalizeKeyword(keyword), null, "PUBLIC", tagFilters,
                safeSortField, safeSortOrder, safeSortMode, safeRandomSeed);
        decorateUrls(result.getRecords());
        decorateViewerInfoBatch(result.getRecords(), true);
        return result;
    }

    public Page<ImageVO> getPublicByUserUuid(String userUuid, Integer page, Integer limit, String keyword, String tags,
                                             String sortMode, String randomSeed, String sortField, String sortOrder) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String normalizedUserUuid = userUuid.trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUuid, normalizedUserUuid));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Page<ImageVO> pageParam = new Page<>(page, normalizeLimit(limit));
        List<String> tagFilters = parseTags(tags);
        String safeSortMode = "random".equalsIgnoreCase(sortMode) ? "random" : "latest";
        String safeSortField = sortField != null && ALLOWED_SQUARE_SORT_FIELDS.contains(sortField) ? sortField : "upload_time";
        String safeSortOrder = "image_name".equals(safeSortField) || "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
        String safeRandomSeed = "random".equals(safeSortMode)
                ? (randomSeed == null || randomSeed.isBlank() ? normalizedUserUuid : randomSeed.trim())
                : null;

        Page<ImageVO> result = imageMapper.selectImageVOPage(
                pageParam, user.getId(), normalizeKeyword(keyword), null, "PUBLIC", tagFilters,
                safeSortField, safeSortOrder, safeSortMode, safeRandomSeed);
        decorateUrls(result.getRecords());
        decorateViewerInfoBatch(result.getRecords(), false);
        return result;
    }

    public byte[] download(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        byte[] bytes = downloadFromStorageOrBase64(image);
        if (bytes == null) {
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
        }
        return bytes;
    }

    public ImageDownloadFile downloadByUuid(String uuid, String format) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        String normalizedFormat = normalizeFormat(format);
        if (normalizedFormat == null) {
            byte[] bytes = downloadFromStorageOrBase64(image);
            if (bytes == null) {
                throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND, "原始图片文件不存在");
            }
            return new ImageDownloadFile(
                    bytes,
                    originalContentType(image),
                    downloadFilename(image, originalExt(image)),
                    cacheControlForDownload(image)
            );
        }

        if ("gif".equals(normalizedFormat)) {
            byte[] originalBytes = downloadFromStorageOrBase64(image);
            if (originalBytes == null) {
                throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND, "原始图片文件不存在");
            }
            boolean gif = ImageConvertUtil.isGif(image.getOriginalContentType(), originalExt(image), originalBytes);
            if (!gif) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持将静态图片转换为 GIF");
            }
            return new ImageDownloadFile(
                    originalBytes,
                    "image/gif",
                    downloadFilename(image, "gif"),
                    cacheControlForDownload(image)
            );
        }

        if (sameOriginalFormat(image, normalizedFormat)) {
            byte[] originalBytes = downloadFromStorageOrBase64(image);
            if (originalBytes == null) {
                throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND, "原始图片文件不存在");
            }
            return new ImageDownloadFile(
                    originalBytes,
                    ImageConvertUtil.contentTypeForExt(normalizedFormat),
                    downloadFilename(image, normalizedFormat),
                    cacheControlForDownload(image)
            );
        }

        String downloadKey = downloadCacheKey(image, normalizedFormat);
        String contentType = ImageConvertUtil.contentTypeForExt(normalizedFormat);
        if (downloadKey != null && storageService.objectExists("images", downloadKey)) {
            return new ImageDownloadFile(
                    storageService.download("images", downloadKey),
                    contentType,
                    downloadFilename(image, normalizedFormat),
                    cacheControlForDownload(image)
            );
        }

        byte[] originalBytes = downloadFromStorageOrBase64(image);
        if (originalBytes == null) {
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND, "原始图片文件不存在");
        }
        boolean gif = ImageConvertUtil.isGif(image.getOriginalContentType(), originalExt(image), originalBytes);
        byte[] converted = convertOriginal(originalBytes, image, normalizedFormat, gif);
        if (downloadKey != null) {
            storageService.upload("images", downloadKey, converted, contentType, ImageUrlService.PRIVATE_CACHE_CONTROL);
        }
        return new ImageDownloadFile(
                converted,
                contentType,
                downloadFilename(image, normalizedFormat),
                cacheControlForDownload(image)
        );
    }

    public byte[] downloadByUuid(String uuid) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        byte[] bytes = downloadFromStorageOrBase64(image);
        if (bytes == null) {
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
        }
        return bytes;
    }

    private byte[] downloadFromStorageOrBase64(Image image) {
        for (String key : originalDownloadKeys(image)) {
            try {
                return storageService.download("images", key);
            } catch (Exception e) {
                // fall through to Base64 fallback if storage download fails
            }
        }
        String imagePath = image.getImagePath();
        if (imagePath != null && imagePath.startsWith("data:image/")) {
            int base64Start = imagePath.indexOf(";base64,");
            if (base64Start > 0) {
                return Base64.getDecoder().decode(imagePath.substring(base64Start + 8));
            }
        }
        return null;
    }

    public Long resolveImageId(String uuid) {
        Image image = imageRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        return image.getId();
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return null;
        }
        List<String> parsed = Arrays.stream(tags.split("#"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
        return parsed.isEmpty() ? null : parsed;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || !ALLOWED_PAGE_SIZES.contains(limit)) {
            return 50;
        }
        return limit;
    }

    private String imageUrlForStorageImage(ImageVO vo) {
        return accessUrlForKey(vo, vo.getStorageKey());
    }

    private void decorateUrls(List<ImageVO> records) {
        for (ImageVO vo : records) {
            decorateUrls(vo);
        }
    }

    private void decorateUrls(ImageVO vo) {
        String originalKey = firstNonBlank(vo.getOriginalKey(), vo.getStorageKey());
        if (originalKey != null && !originalKey.isBlank()) {
            String originalUrl = accessUrlForKey(vo, originalKey);
            vo.setOriginalUrl(originalUrl);
            vo.setImageUrl(originalUrl);
            if ("PUBLIC".equals(vo.getVisibility())) {
                vo.setPublicUrl(originalUrl);
            } else {
                vo.setPrivateUrl(originalUrl);
            }
        } else if (vo.getImagePath() != null && vo.getImagePath().startsWith("data:image/")) {
            String downloadUrl = "/api/image/download/" + vo.getUuid();
            vo.setImageUrl(downloadUrl);
            vo.setOriginalUrl(downloadUrl);
        }

        if (vo.getMediumKey() != null && !vo.getMediumKey().isBlank()) {
            vo.setMediumUrl(accessUrlForKey(vo, vo.getMediumKey()));
        }
        if (vo.getThumbKey() != null && !vo.getThumbKey().isBlank()) {
            vo.setThumbUrl(accessUrlForKey(vo, vo.getThumbKey()));
        }
    }

    private String accessUrlForKey(ImageVO vo, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        if ("PUBLIC".equals(vo.getVisibility())) {
            return imageUrlService.getPublicImageUrl(storageKey, vo.getMediaVersion());
        }
        return imageUrlService.getPrivateImageUrl(storageKey);
    }

    private void decorateViewerInfoBatch(List<ImageVO> records, boolean includeCommentCounts) {
        if (records == null || records.isEmpty()) return;

        List<Long> imageIds = records.stream().map(ImageVO::getId).filter(id -> id != null).toList();
        if (imageIds.isEmpty()) return;

        // Batch fetch like counts: 1 query
        Map<Long, Long> likeCounts;
        try {
            List<Map<String, Object>> counts = imageMapper.countByImageIds(imageIds);
            likeCounts = counts.stream().collect(Collectors.toMap(
                    m -> ((Number) m.get("image_id")).longValue(),
                    m -> ((Number) m.get("cnt")).longValue(),
                    (a, b) -> a
            ));
        } catch (Exception e) {
            likeCounts = Collections.emptyMap();
        }

        // Batch fetch comment counts: 1 query
        Map<Long, Long> commentCounts = Collections.emptyMap();
        if (includeCommentCounts) {
            try {
                List<Map<String, Object>> counts = imageMapper.countCommentsByImageIds(imageIds);
                commentCounts = counts.stream().collect(Collectors.toMap(
                        m -> ((Number) m.get("image_id")).longValue(),
                        m -> ((Number) m.get("cnt")).longValue(),
                        (a, b) -> a
                ));
            } catch (Exception ignored) {
                // Keep the Explore page available even if interaction counts cannot be loaded.
            }
        }

        // Batch fetch user-liked status: 1 query (only if logged in)
        Set<Long> likedByMeIds;
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            try {
                List<Long> likedIds = imageMapper.findLikedImageIdsByUser(imageIds, userId);
                likedByMeIds = Set.copyOf(likedIds);
            } catch (Exception e) {
                likedByMeIds = Collections.emptySet();
            }
        } else {
            likedByMeIds = Collections.emptySet();
        }

        // Apply to VOs
        for (ImageVO vo : records) {
            if (vo.getId() != null) {
                vo.setLikeCount(likeCounts.getOrDefault(vo.getId(), 0L));
                if (includeCommentCounts) {
                    vo.setCommentCount(commentCounts.getOrDefault(vo.getId(), 0L));
                }
                vo.setLikedByMe(likedByMeIds.contains(vo.getId()));
            }
            vo.setOwnedByMe(permissionService.isOwner(vo.getUserId()));
            vo.setEditableByMe(permissionService.canEdit(vo.getUserId()));
        }
    }

    private void decorateViewerInfo(ImageVO vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        vo.setOwnedByMe(permissionService.isOwner(vo.getUserId()));
        vo.setEditableByMe(permissionService.canEdit(vo.getUserId()));
        Long count = imageLikeMapper.countByImageId(vo.getId());
        vo.setLikeCount(count == null ? 0L : count);
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            Long liked = imageLikeMapper.countByImageIdAndUserId(vo.getId(), userId);
            vo.setLikedByMe(liked != null && liked > 0);
        } else {
            vo.setLikedByMe(false);
        }
    }

    private List<String> originalDownloadKeys(Image image) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        addKey(keys, image.getOriginalKey());
        addKey(keys, image.getStorageKey());
        return new ArrayList<>(keys);
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return null;
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        if ("jpeg".equals(normalized)) {
            normalized = "jpg";
        }
        if (!Set.of("jpg", "png", "gif").contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的下载格式");
        }
        return normalized;
    }

    private byte[] convertOriginal(byte[] originalBytes, Image image, String targetFormat, boolean gif) {
        if ("jpg".equals(targetFormat)) {
            return gif
                    ? ImageConvertUtil.extractGifFirstFrameToJpg(originalBytes)
                    : ImageConvertUtil.convertToJpg(originalBytes);
        }
        if ("png".equals(targetFormat)) {
            return gif
                    ? ImageConvertUtil.extractGifFirstFrameToPng(originalBytes)
                    : ImageConvertUtil.convertToPng(originalBytes);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的下载格式");
    }

    private boolean sameOriginalFormat(Image image, String targetFormat) {
        String ext = originalExt(image);
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        return targetFormat.equals(ext) && ("jpg".equals(targetFormat) || "png".equals(targetFormat));
    }

    private String originalContentType(Image image) {
        if (image.getOriginalContentType() != null && !image.getOriginalContentType().isBlank()) {
            return image.getOriginalContentType();
        }
        return ImageConvertUtil.contentTypeForExt(originalExt(image));
    }

    private String originalExt(Image image) {
        if (image.getOriginalExt() != null && !image.getOriginalExt().isBlank()) {
            return normalizeExt(image.getOriginalExt());
        }
        if (image.getImageType() != null && !image.getImageType().isBlank()) {
            return normalizeExt(image.getImageType());
        }
        String filename = firstNonBlank(image.getOriginalFilename(), image.getImageName());
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1 ? normalizeExt(filename.substring(dot + 1)) : "bin";
    }

    private String downloadFilename(Image image, String ext) {
        String source = firstNonBlank(image.getOriginalFilename(), image.getImageName());
        String base = source == null || source.isBlank() ? "image-" + image.getUuid() : source;
        base = base.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        base = base.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (base.isBlank()) {
            base = "image-" + image.getUuid();
        }
        return base + "." + normalizeExt(ext);
    }

    private String downloadCacheKey(Image image, String targetFormat) {
        String key = firstNonBlank(image.getOriginalKey(), image.getStorageKey());
        if (key != null && key.contains("/original.")) {
            return key.substring(0, key.lastIndexOf("/original.")) + "/download/original." + targetFormat;
        }
        if (image.getUuid() != null && !image.getUuid().isBlank()) {
            return "images/" + image.getUuid() + "/download/original." + targetFormat;
        }
        return null;
    }

    private String cacheControlForDownload(Image image) {
        return "PUBLIC".equals(image.getVisibility())
                ? "public, max-age=604800"
                : ImageUrlService.PRIVATE_CACHE_CONTROL;
    }

    private String normalizeExt(String ext) {
        String value = ext == null ? "" : ext.trim().toLowerCase(Locale.ROOT);
        return "jpeg".equals(value) ? "jpg" : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private void addKey(Set<String> keys, String key) {
        if (key != null && !key.isBlank()) {
            keys.add(key);
        }
    }
}
