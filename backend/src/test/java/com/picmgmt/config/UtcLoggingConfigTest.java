package com.picmgmt.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class UtcLoggingConfigTest {

    private static final Pattern UTC_LOG_LINE = Pattern.compile(
            "(?m)^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z\\s+INFO.*UTC_LOG_PROBE$"
    );

    @Test
    void dockerProfileWritesUtcIsoTimestampsToStdout(CapturedOutput output) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(LoggingProbe.class)
                .profiles("docker")
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run("--spring.main.banner-mode=off")) {
            LoggerFactory.getLogger(UtcLoggingConfigTest.class).info("UTC_LOG_PROBE");
        }

        assertTrue(UTC_LOG_LINE.matcher(output.getOut()).find(), output.getOut());
    }

    @SpringBootConfiguration
    static class LoggingProbe {}
}
