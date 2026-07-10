package com.picmgmt.storage;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadRejectsPathThatEscapesBucketRoot() throws Exception {
        Path basePath = tempDir.resolve("storage");
        Files.createDirectories(basePath);
        Files.writeString(basePath.resolve("secret.txt"), "secret", StandardCharsets.UTF_8);
        LocalStorageService service = service(basePath);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.download("comments", "../secret.txt")
        );

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    }

    @Test
    void nestedObjectKeyStillRoundTripsInsideBucket() {
        LocalStorageService service = service(tempDir.resolve("storage"));
        byte[] bytes = "safe".getBytes(StandardCharsets.UTF_8);

        service.upload("comments", "comments/nested/image.png", bytes, "image/png");

        assertArrayEquals(bytes, service.download("comments", "comments/nested/image.png"));
    }

    private LocalStorageService service(Path basePath) {
        LocalStorageService service = new LocalStorageService();
        ReflectionTestUtils.setField(service, "basePath", basePath.toString());
        return service;
    }
}
