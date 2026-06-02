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
import java.util.List;
import java.util.Set;

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
        for (ImageVO vo : result.getRecords()) {
            if (vo.getStorageKey() != null && !vo.getStorageKey().isBlank()) {
                vo.setImageUrl(imageUrlForStorageImage(vo));
            } else if (vo.getImagePath() != null && vo.getImagePath().startsWith("data:image/")) {
                vo.setImageUrl("/api/image/download/" + vo.getUuid());
            }
            decorateViewerInfo(vo);
        }
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
        for (ImageVO vo : result.getRecords()) {
            if (vo.getStorageKey() != null && !vo.getStorageKey().isBlank()) {
                vo.setImageUrl(imageUrlForStorageImage(vo));
            } else if (vo.getImagePath() != null && vo.getImagePath().startsWith("data:image/")) {
                vo.setImageUrl("/api/image/download/" + vo.getUuid());
            }
            decorateViewerInfo(vo);
        }
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
        for (ImageVO vo : result.getRecords()) {
            if (vo.getStorageKey() != null && !vo.getStorageKey().isBlank()) {
                vo.setImageUrl(imageUrlForStorageImage(vo));
            } else if (vo.getImagePath() != null && vo.getImagePath().startsWith("data:image/")) {
                vo.setImageUrl("/api/image/download/" + vo.getUuid());
            }
            decorateViewerInfo(vo);
        }
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
        if (image.getStorageKey() != null && !image.getStorageKey().isBlank()) {
            try {
                return storageService.download("images", image.getStorageKey());
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
        if ("PUBLIC".equals(vo.getVisibility())) {
            String publicUrl = imageUrlService.getPublicImageUrl(vo.getStorageKey(), vo.getMediaVersion());
            vo.setPublicUrl(publicUrl);
            return publicUrl;
        }
        String privateUrl = imageUrlService.getPrivateImageUrl(vo.getStorageKey());
        vo.setPrivateUrl(privateUrl);
        return privateUrl;
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
}
