package com.picmgmt.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerProfileConfigTest {

    @Test
    void dockerProfile_shouldBindRedisPasswordFromEnvironment() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application-docker.yml"));

        assertTrue(config.contains("password: ${REDIS_PASSWORD:}"));
        assertTrue(config.contains("dateformat: \"yyyy-MM-dd'T'HH:mm:ss.SSSXXX,UTC\""));
    }
}
