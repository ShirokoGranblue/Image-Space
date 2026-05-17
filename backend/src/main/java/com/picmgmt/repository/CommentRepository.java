package com.picmgmt.repository;

import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Comment;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final CommentMapper commentMapper;
    private final StorageService storageService;
    private final CacheService cacheService;

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String LIST_KEY_PREFIX = "comment:list:";

    public void insert(Comment comment) {
        commentMapper.insert(comment);
        cacheService.evict(LIST_KEY_PREFIX + comment.getImageId());
    }

    public void deleteById(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        commentMapper.deleteById(commentId);
        if (comment != null) {
            cacheService.evict(LIST_KEY_PREFIX + comment.getImageId());
            if (comment.getImageKey() != null) {
                storageService.delete("comments", comment.getImageKey());
            }
        }
    }

    public List<CommentVO> listByImageId(Long imageId) {
        String key = LIST_KEY_PREFIX + imageId;
        return cacheService.getOrLoad(key, (Class<List<CommentVO>>)(Class<?>)List.class,
                () -> {
                    List<CommentVO> list = commentMapper.selectCommentVOList(imageId);
                    for (CommentVO vo : list) {
                        if (vo.getImagePath() != null) {
                            vo.setImageUrl(storageService.getAccessUrl("comments", vo.getImagePath()));
                        }
                    }
                    return list;
                }, TTL);
    }

    public Comment findById(Long id) {
        return commentMapper.selectById(id);
    }
}
