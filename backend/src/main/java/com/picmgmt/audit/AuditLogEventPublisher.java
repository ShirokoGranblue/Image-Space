package com.picmgmt.audit;

import com.picmgmt.entity.AuditLog;
import com.picmgmt.vo.AuditLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class AuditLogEventPublisher {

    private static final long TIMEOUT_MILLIS = 0L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void publishIfImportant(AuditLog logEntry) {
        if (!shouldPublish(logEntry) || emitters.isEmpty()) {
            return;
        }
        AuditLogVO event = AuditLogVO.from(logEntry);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("audit-risk")
                        .id(String.valueOf(logEntry.getId()))
                        .data(event));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing stale audit SSE emitter: {}", e.getMessage());
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }

    @Scheduled(fixedRateString = "${audit.sse.heartbeat-ms:15000}")
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing stale audit SSE emitter: {}", e.getMessage());
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }

    private boolean shouldPublish(AuditLog logEntry) {
        if (logEntry == null) {
            return false;
        }
        return "HIGH".equalsIgnoreCase(logEntry.getRiskLevel())
                || "FAILED".equalsIgnoreCase(logEntry.getStatus())
                || "FAIL".equalsIgnoreCase(logEntry.getResult());
    }
}
