package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageReadService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final ImageLikeMapper imageLikeMapper;
    private final ImagePermissionService permissionService;
    private final StorageService storageService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("upload_time", "image_name", "file_size");
    private static final Set<String> ALLOWED_SQUARE_SORT_FIELDS = Set.of("upload_time", "file_size", "image_name");
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(30, 50, 100);

    public ImageReadService(ImageRepository imageRepository, ImageMapper imageMapper, ImageLikeMapper imageLikeMapper,
                            ImagePermissionService permissionService, StorageService storageService) {
        this.imageRepository = imageRepository;
        this.imageMapper = imageMapper;
        this.imageLikeMapper = imageLikeMapper;
        this.permissionService = permissionService;
        this.storageService = storageService;
    }
    
    public ImageVO getById(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        ImageVO vo = imageRepository.toVO(image);
        decorateLikeInfo(vo);
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
            if (vo.getStorageKey() != null) {
                vo.setImageUrl(storageService.getPresignedUrl("images", vo.getStorageKey(), java.time.Duration.ofMinutes(5)));
            }
            decorateLikeInfo(vo);
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
            if (vo.getStorageKey() != null) {
                vo.setImageUrl(storageService.getPresignedUrl("images", vo.getStorageKey(), java.time.Duration.ofMinutes(5)));
            }
            decorateLikeInfo(vo);
        }
        return result;
    }

    public byte[] download(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        return storageService.download("images", image.getStorageKey());
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

    private void decorateLikeInfo(ImageVO vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
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
