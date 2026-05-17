package com.picmgmt.config;

import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.ImageCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * 启动时自动将旧的本地文件路径迁移为 Base64 Data URL 存入数据库。
 * 仅处理 image_path / avatar / background 以 "/upload/" 开头的旧数据。
 * 迁移完成后可安全删除 upload 目录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.upload-path:./upload}")
    private String uploadPath;

    @Override
    public void run(String... args) {
        log.info("===== 开始检查并适配数据库表结构 =====");
        try {
            // 将路径列类型改为 LONGTEXT 以容纳 Base64
            jdbcTemplate.execute("ALTER TABLE images MODIFY COLUMN image_path LONGTEXT NOT NULL");
            log.info("已将 images.image_path 列修改为 LONGTEXT");
            
            // 尝试添加遗漏的列（捕获 Duplicate column name 异常）
            addColumnSafely("users", "display_name", "VARCHAR(50)");
            addColumnSafely("users", "avatar", "LONGTEXT");
            addColumnSafely("users", "email", "VARCHAR(100)");
            addColumnSafely("users", "phone", "VARCHAR(20)");
            addColumnSafely("users", "bio", "VARCHAR(200)");
            addColumnSafely("users", "background", "LONGTEXT");
            addColumnSafely("comments", "image_path", "LONGTEXT");
            
            // 用户表原有字段如果存在，且是 VARCHAR，改为 LONGTEXT
            try { jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN avatar LONGTEXT"); } catch (Exception e) {}
            try { jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN background LONGTEXT"); } catch (Exception e) {}
            
        } catch (Exception e) {
            log.warn("表结构适配出现异常 (请确保你使用的是 MySQL): {}", e.getMessage());
        }

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        if (!basePath.toFile().exists()) {
            log.info("upload 目录不存在，跳过数据迁移");
            return;
        }

        log.info("===== 开始检查并迁移旧文件数据到 Base64 =====");
        int migratedImages = migrateImages(basePath);
        int migratedUsers = migrateUsers(basePath);
        int migratedComments = migrateComments(basePath);

        if (migratedImages + migratedUsers + migratedComments > 0) {
            log.info("迁移完成: 图片 {} 条, 用户头像/背景 {} 条, 评论图片 {} 条",
                    migratedImages, migratedUsers, migratedComments);
            // 迁移完成后删除 upload 目录
            deleteDirectory(basePath.toFile());
            // 同时删除项目根目录下的 upload 目录
            Path rootUpload = Paths.get("upload").toAbsolutePath().normalize();
            if (rootUpload.toFile().exists()) {
                deleteDirectory(rootUpload.toFile());
            }
            log.info("已删除 upload 目录");
        } else {
            log.info("无需迁移的旧数据");
            // 即使没有需要迁移的数据，也尝试删除空的 upload 目录
            if (basePath.toFile().exists()) {
                deleteDirectory(basePath.toFile());
                log.info("已删除空的 upload 目录");
            }
        }
        log.info("===== 数据迁移检查完毕 =====");
    }

    private int migrateImages(Path basePath) {
        List<Image> images = imageMapper.selectList(null);
        int count = 0;
        for (Image image : images) {
            String path = image.getImagePath();
            if (path != null && path.startsWith("/upload/") && !ImageCacheService.isDataUrl(path)) {
                String relativePath = path.replace("/upload", "");
                File file = basePath.resolve(relativePath.startsWith("/") ? relativePath.substring(1) : relativePath).normalize().toFile();
                if (file.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        String ext = getExtension(file.getName());
                        String mimeType = ImageCacheService.getMimeType(ext);
                        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
                        image.setImagePath(dataUrl);
                        imageMapper.updateById(image);
                        count++;
                        log.debug("迁移图片: {} -> Base64 ({})", path, formatSize(bytes.length));
                    } catch (Exception e) {
                        log.warn("迁移图片失败: {} - {}", path, e.getMessage());
                    }
                } else {
                    log.warn("图片文件不存在: {}", file.getAbsolutePath());
                }
            }
        }
        return count;
    }

    private int migrateUsers(Path basePath) {
        List<User> users = userMapper.selectList(null);
        int count = 0;
        for (User user : users) {
            boolean updated = false;

            // 迁移头像
            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/upload/") && !ImageCacheService.isDataUrl(avatar)) {
                String dataUrl = fileToDataUrl(basePath, avatar);
                if (dataUrl != null) {
                    user.setAvatar(dataUrl);
                    updated = true;
                    log.debug("迁移头像: userId={}", user.getId());
                }
            }

            // 迁移背景
            String background = user.getBackground();
            if (background != null && background.startsWith("/upload/") && !ImageCacheService.isDataUrl(background)) {
                String dataUrl = fileToDataUrl(basePath, background);
                if (dataUrl != null) {
                    user.setBackground(dataUrl);
                    updated = true;
                    log.debug("迁移背景: userId={}", user.getId());
                }
            }

            if (updated) {
                userMapper.updateById(user);
                count++;
            }
        }
        return count;
    }

    private int migrateComments(Path basePath) {
        List<Comment> comments = commentMapper.selectList(null);
        int count = 0;
        for (Comment comment : comments) {
            String path = comment.getImagePath();
            if (path != null && path.startsWith("/upload/") && !ImageCacheService.isDataUrl(path)) {
                String dataUrl = fileToDataUrl(basePath, path);
                if (dataUrl != null) {
                    comment.setImagePath(dataUrl);
                    commentMapper.updateById(comment);
                    count++;
                    log.debug("迁移评论图片: commentId={}", comment.getId());
                }
            }
        }
        return count;
    }

    private String fileToDataUrl(Path basePath, String uploadPath) {
        String relativePath = uploadPath.replace("/upload", "");
        File file = basePath.resolve(relativePath.startsWith("/") ? relativePath.substring(1) : relativePath).normalize().toFile();
        if (!file.exists()) {
            log.warn("文件不存在: {}", file.getAbsolutePath());
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String ext = getExtension(file.getName());
            String mimeType = ImageCacheService.getMimeType(ext);
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("文件读取失败: {} - {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    private void addColumnSafely(String tableName, String columnName, String columnType) {
        try {
            jdbcTemplate.execute(String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType));
            log.info("已向表 {} 添加列 {}", tableName, columnName);
        } catch (Exception e) {
            // 忽略重复列的异常 (Error 1060: Duplicate column name)
            if (!e.getMessage().contains("Duplicate column name")) {
                log.warn("添加列 {}.{} 失败: {}", tableName, columnName, e.getMessage());
            }
        }
    }
}
