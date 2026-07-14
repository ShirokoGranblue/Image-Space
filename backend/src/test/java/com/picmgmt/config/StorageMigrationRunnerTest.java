package com.picmgmt.config;

import com.picmgmt.entity.Image;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageMigrationRunnerTest {

    @Mock private ImageMapper imageMapper;
    @Mock private UserMapper userMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private StorageService storageService;

    @AfterEach
    void tearDown() {
        System.clearProperty("storage_migration_done");
    }

    @Test
    void run_shouldConvertLegacyCommaTagsToHashTagsOnce() {
        Image legacy = new Image();
        legacy.setId(1L);
        legacy.setTags("cute, blue,,avatar");
        Image alreadyHashSeparated = new Image();
        alreadyHashSeparated.setId(2L);
        alreadyHashSeparated.setTags("cute#blue");

        when(imageMapper.selectList(null)).thenReturn(List.of(legacy, alreadyHashSeparated));
        when(userMapper.selectList(null)).thenReturn(List.of());
        when(commentMapper.selectList(null)).thenReturn(List.of());

        new StorageMigrationRunner(imageMapper, userMapper, commentMapper, storageService).run();

        ArgumentCaptor<Image> updatedImage = ArgumentCaptor.forClass(Image.class);
        verify(imageMapper).updateById(updatedImage.capture());
        assertEquals(1L, updatedImage.getValue().getId());
        assertEquals("cute#blue#avatar", updatedImage.getValue().getTags());
    }

    @Test
    void run_shouldSkipTagValuesThatAlreadyContainHash() {
        Image image = new Image();
        image.setId(1L);
        image.setTags("cute#blue,avatar");

        when(imageMapper.selectList(null)).thenReturn(List.of(image));
        when(userMapper.selectList(null)).thenReturn(List.of());
        when(commentMapper.selectList(null)).thenReturn(List.of());

        new StorageMigrationRunner(imageMapper, userMapper, commentMapper, storageService).run();

        verify(imageMapper, never()).updateById(any(Image.class));
    }

    @Test
    void run_shouldRepairMissingAvatarObjectEvenWhenKeyExists() {
        User user = new User();
        user.setId(7L);
        user.setAvatarKey("avatars/existing-key.png");
        user.setAvatar(dataUri("image/png", "avatar"));

        when(imageMapper.selectList(null)).thenReturn(List.of());
        when(userMapper.selectList(null)).thenReturn(List.of(user));
        when(commentMapper.selectList(null)).thenReturn(List.of());
        when(storageService.objectExists("avatars", "avatars/existing-key.png")).thenReturn(false);

        new StorageMigrationRunner(imageMapper, userMapper, commentMapper, storageService).run();

        verify(storageService).upload(
                "avatars",
                "avatars/existing-key.png",
                "avatar".getBytes(StandardCharsets.UTF_8),
                "image/png"
        );
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void run_shouldSkipAvatarWhenKeyAndObjectBothExist() {
        User user = new User();
        user.setId(7L);
        user.setAvatarKey("avatars/existing-key.png");
        user.setAvatar(dataUri("image/png", "avatar"));

        when(imageMapper.selectList(null)).thenReturn(List.of());
        when(userMapper.selectList(null)).thenReturn(List.of(user));
        when(commentMapper.selectList(null)).thenReturn(List.of());
        when(storageService.objectExists("avatars", "avatars/existing-key.png")).thenReturn(true);

        new StorageMigrationRunner(imageMapper, userMapper, commentMapper, storageService).run();

        verify(storageService, never()).upload(any(), any(), any(), any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    private String dataUri(String contentType, String value) {
        return "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
