package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.ImageLike;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.ImageLikeService;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.ImageLikeStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ImageLikeServiceImpl implements ImageLikeService {

    private final ImageLikeMapper imageLikeMapper;
    private final ImageRepository imageRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ImageLikeStatusVO like(Long imageId) {
        long userId = StpUtil.getLoginIdAsLong();
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        ImageLike existing = imageLikeMapper.findByImageIdAndUserId(imageId, userId);
        if (existing == null) {
            ImageLike like = new ImageLike();
            like.setImageId(imageId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            imageLikeMapper.insert(like);
            if (!image.getUserId().equals(userId)) {
                notificationService.createLikeNotification(image, userId);
            }
        }
        return status(imageId, true);
    }

    @Override
    @Transactional
    public ImageLikeStatusVO unlike(Long imageId) {
        long userId = StpUtil.getLoginIdAsLong();
        imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        imageLikeMapper.deleteByImageIdAndUserId(imageId, userId);
        return status(imageId, false);
    }

    private ImageLikeStatusVO status(Long imageId, boolean likedByMe) {
        Long count = imageLikeMapper.countByImageId(imageId);
        return new ImageLikeStatusVO(imageId, count == null ? 0L : count, likedByMe);
    }
}
