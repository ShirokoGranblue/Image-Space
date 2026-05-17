package com.picmgmt.config;

import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class StorageMigrationRunner implements CommandLineRunner {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final StorageService storageService;

    private static final String MIGRATION_FLAG = "storage_migration_done";

    @Override
    public void run(String... args) {
        if ("1".equals(System.getProperty(MIGRATION_FLAG))) {
            log.info("数据迁移已完成，跳过");
            return;
        }

        log.info("===== 开始迁移 Base64 → MinIO =====");
        int imageCount = migrateImages();
        int avatarCount = migrateAvatars();
        int bgCount = migrateBackgrounds();
        int commentCount = migrateCommentImages();
        System.setProperty(MIGRATION_FLAG, "1");
        log.info("===== 迁移完成: 图片 {} 张, 头像 {} 个, 背景 {} 个, 评论 {} 条 =====",
                imageCount, avatarCount, bgCount, commentCount);

        if (imageCount + avatarCount + bgCount + commentCount > 0) {
            log.info("旧 Base64 列仍保留。确认系统正常后，手动执行 DROP COLUMN:");
            log.info("  ALTER TABLE images DROP COLUMN image_path;");
            log.info("  ALTER TABLE users DROP COLUMN avatar, DROP COLUMN background;");
            log.info("  ALTER TABLE comments DROP COLUMN image_path;");
        }
    }

    private int migrateImages() {
        List<Image> images = imageMapper.selectList(null);
        int count = 0;
        for (Image image : images) {
            if (image.getImagePath() == null || !image.getImagePath().startsWith("data:")) continue;
            if (image.getStorageKey() != null) continue; // already migrated
            try {
                String base64Data = image.getImagePath().substring(image.getImagePath().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String ext = image.getImageType().toLowerCase();
                String storageKey = "images/" + image.getUserId() + "/" + image.getId() + "." + ext;
                storageService.upload("images", storageKey, bytes,
                        "image/" + (ext.equals("jpg") ? "jpeg" : ext));
                image.setStorageKey(storageKey);
                imageMapper.updateById(image);
                count++;
            } catch (Exception e) {
                log.warn("迁移图片 {} 失败: {}", image.getId(), e.getMessage());
            }
        }
        return count;
    }

    private int migrateAvatars() {
        List<User> users = userMapper.selectList(null);
        int count = 0;
        for (User user : users) {
            if (user.getAvatar() == null || !user.getAvatar().startsWith("data:")) continue;
            if (user.getAvatarKey() != null) continue;
            try {
                String base64Data = user.getAvatar().substring(user.getAvatar().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String storageKey = "avatars/" + user.getId() + "/avatar";
                storageService.upload("avatars", storageKey, bytes, "image/png");
                user.setAvatarKey(storageKey);
                userMapper.updateById(user);
                count++;
            } catch (Exception e) {
                log.warn("迁移用户头像 {} 失败: {}", user.getId(), e.getMessage());
            }
        }
        return count;
    }

    private int migrateBackgrounds() {
        List<User> users = userMapper.selectList(null);
        int count = 0;
        for (User user : users) {
            if (user.getBackground() == null || !user.getBackground().startsWith("data:")) continue;
            if (user.getBackgroundKey() != null) continue;
            try {
                String base64Data = user.getBackground().substring(user.getBackground().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String storageKey = "backgrounds/" + user.getId() + "/background";
                storageService.upload("backgrounds", storageKey, bytes, "image/jpeg");
                user.setBackgroundKey(storageKey);
                userMapper.updateById(user);
                count++;
            } catch (Exception e) {
                log.warn("迁移用户背景 {} 失败: {}", user.getId(), e.getMessage());
            }
        }
        return count;
    }

    private int migrateCommentImages() {
        List<Comment> comments = commentMapper.selectList(null);
        int count = 0;
        for (Comment comment : comments) {
            if (comment.getImagePath() == null || !comment.getImagePath().startsWith("data:")) continue;
            if (comment.getImageKey() != null) continue;
            try {
                String base64Data = comment.getImagePath().substring(comment.getImagePath().indexOf(',') + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                String storageKey = "comments/" + comment.getId();
                storageService.upload("comments", storageKey, bytes, "image/png");
                comment.setImageKey(storageKey);
                commentMapper.updateById(comment);
                count++;
            } catch (Exception e) {
                log.warn("迁移评论图片 {} 失败: {}", comment.getId(), e.getMessage());
            }
        }
        return count;
    }
}
