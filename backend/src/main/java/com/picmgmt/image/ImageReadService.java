package com.picmgmt.image;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.ImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageReadService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final ImagePermissionService permissionService;
    private final StorageService storageService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("upload_time", "image_name", "file_size");

    public ImageVO getById(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!permissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        return imageRepository.toVO(image);
    }

    public Page<ImageVO> page(ImageQueryDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        String sortField = ALLOWED_SORT_FIELDS.contains(dto.getSortField()) ? dto.getSortField() : "upload_time";
        String sortOrder = "asc".equalsIgnoreCase(dto.getSortOrder()) ? "asc" : "desc";

        Page<ImageVO> pageParam = new Page<>(dto.getPage(), dto.getLimit());
        Page<ImageVO> result = imageMapper.selectImageVOPage(
                pageParam, userId, dto.getKeyword(), dto.getCategoryId(), null, sortField, sortOrder);
        for (ImageVO vo : result.getRecords()) {
            if (vo.getStorageKey() != null) {
                vo.setImageUrl(storageService.getAccessUrl("images", vo.getStorageKey()));
            }
        }
        return result;
    }

    public Page<ImageVO> getSquare(Integer page, Integer limit) {
        Page<ImageVO> pageParam = new Page<>(page, limit);
        Page<ImageVO> result = imageMapper.selectImageVOPage(
                pageParam, null, null, null, "PUBLIC", "upload_time", "desc");
        for (ImageVO vo : result.getRecords()) {
            if (vo.getStorageKey() != null) {
                vo.setImageUrl(storageService.getAccessUrl("images", vo.getStorageKey()));
            }
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
}
