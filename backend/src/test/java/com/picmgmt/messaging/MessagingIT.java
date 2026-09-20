package com.picmgmt.messaging;

import com.picmgmt.audit.AuditLogEventPublisher;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.AuditLog;
import com.picmgmt.image.AsyncImageService;
import com.picmgmt.image.MediaMetaCacheService;
import com.picmgmt.storage.LocalStorageService;
import com.picmgmt.storage.StorageService;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real MySQL/RabbitMQ test; run using the messaging-it Maven profile and isolated Compose. */
@SpringBootTest(classes = MessagingIT.Config.class, properties = {
        "spring.config.location=optional:classpath:/mq-empty.yml",
        "spring.datasource.url=jdbc:mysql://127.0.0.1:13316/mq_integration?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false",
        "spring.datasource.username=root", "spring.datasource.password=${MQ_TEST_PASSWORD}",
        "spring.flyway.locations=classpath:db/migration/versioned",
        "spring.rabbitmq.host=127.0.0.1", "spring.rabbitmq.port=15673",
        "spring.rabbitmq.username=mq-test", "spring.rabbitmq.password=${MQ_TEST_PASSWORD}",
        "spring.rabbitmq.virtual-host=image-space-it", "spring.rabbitmq.publisher-confirm-type=correlated",
        "spring.rabbitmq.publisher-returns=true", "spring.rabbitmq.listener.simple.auto-startup=false",
        "messaging.enabled=true", "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "storage.local.base-path=${java.io.tmpdir}/image-space-mq-it-${random.uuid}"
})
class MessagingIT {
    @Configuration
    @EnableAutoConfiguration
    @MapperScan("com.picmgmt.mapper")
    @Import({RabbitConfiguration.class, OutboxService.class, OutboxPublisher.class, MessageProcessor.class,
            MessageFailureService.class, MessageConsumers.class, AsyncImageService.class})
    static class Config {
        @Bean CacheService cache() { return mock(CacheService.class); }
        @Bean MediaMetaCacheService mediaCache() { return mock(MediaMetaCacheService.class); }
        @Bean AuditLogEventPublisher auditEvents() { return mock(AuditLogEventPublisher.class); }
        @Bean StorageService storage() throws Exception {
            LocalStorageService storage = new LocalStorageService();
            return spy(storage);
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxService outbox;
    @Autowired OutboxPublisher publisher;
    @Autowired MessageProcessor processor;
    @Autowired MessageFailureService failures;
    @Autowired PlatformTransactionManager transactions;
    @Autowired RabbitTemplate rabbit;
    @Autowired org.springframework.amqp.core.AmqpAdmin admin;
    @Autowired RabbitListenerEndpointRegistry listeners;
    @Autowired AuditLogEventPublisher auditEvents;
    @Autowired StorageService storage;
    @Autowired CacheService cache;

    @BeforeEach void resetState() {
        listeners.stop();
        ((RabbitAdmin) admin).initialize();
        for (String queue : new String[]{RabbitConfiguration.IMAGE_QUEUE, RabbitConfiguration.AUDIT_QUEUE,
                RabbitConfiguration.IMAGE_QUEUE + ".dead", RabbitConfiguration.AUDIT_QUEUE + ".dead"}) admin.purgeQueue(queue);
        jdbc.update("DELETE FROM message_outbox");
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM images");
        reset(auditEvents);
        reset(storage);
        reset(cache);
    }
    private AuditLog audit() {
        AuditLog a = new AuditLog();
        a.setAction("TEST"); a.setModule("TEST"); a.setResult("FAIL"); a.setStatus("FAILED");
        a.setRiskLevel("HIGH"); a.setCostTime(13L); a.setCreateTime(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        a.setRequestParams("{\"password\":\"[FILTERED]\"}");
        return a;
    }
    private String state(String id) { return jdbc.queryForObject("SELECT state FROM message_outbox WHERE event_id=?", String.class, id); }
    private void due(String id) { jdbc.update("UPDATE message_outbox SET next_attempt_at=CURRENT_TIMESTAMP WHERE event_id=?", id); }
    private String token(String id) { return jdbc.queryForObject("SELECT publish_token FROM message_outbox WHERE event_id=?", String.class, id); }

    @Test void auditIndependentTransactionSurvivesBusinessRollbackAndDeduplicates() throws Exception {
        AuditLog a = audit();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            outbox.audit(a);
            throw new IllegalStateException("business rollback");
        }));
        assertEquals("PENDING", state(a.getEventId()));
        doAnswer(call -> {
            TransactionTemplate independent = new TransactionTemplate(transactions);
            independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            independent.executeWithoutResult(status -> {
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class));
                assertEquals("DONE", state(a.getEventId()));
            });
            return null;
        }).when(auditEvents).publishIfImportant(any());
        publisher.publishDue();
        assertEquals("SENT", state(a.getEventId()));
        String dispatch = token(a.getEventId());
        processor.process(a.getEventId(), dispatch, "audit");
        processor.process(a.getEventId(), dispatch, "audit");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class));
        assertEquals(13L, jdbc.queryForObject("SELECT cost_time FROM audit_log", Long.class));
        assertEquals(a.getCreateTime(), jdbc.queryForObject("SELECT create_time FROM audit_log", LocalDateTime.class));
        verify(auditEvents, times(1)).publishIfImportant(any());
    }

    @Test void realBrokerConsumerPersistsAudit() throws Exception {
        AuditLog a = audit(); outbox.audit(a);
        listeners.start(); publisher.publishDue();
        awaitDone(a.getEventId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_id=?", Integer.class, a.getEventId()));
    }

    @Test void unroutableMessageRemainsPending() {
        admin.deleteQueue(RabbitConfiguration.AUDIT_QUEUE);
        try {
            AuditLog a = audit(); outbox.audit(a); publisher.publishDue();
            assertEquals("PENDING", state(a.getEventId()));
        } finally { ((RabbitAdmin) admin).initialize(); }
    }

    @Test void retriesSurviveConsumerRestartAndDeadLetterCanBeReplayed() throws Exception {
        AuditLog a = audit(); outbox.audit(a);
        String id = a.getEventId();
        for (int attempt = 0; attempt < 4; attempt++) {
            due(id); publisher.publishDue();
            failures.failed(id, token(id), new IllegalStateException("transient"));
            assertEquals(attempt < 3 ? "PENDING" : "DEAD_PENDING", state(id));
        }
        due(id); publisher.publishDue();
        assertEquals("DEAD", state(id));
        var dead = rabbit.receive(RabbitConfiguration.AUDIT_QUEUE + ".dead", 5000);
        assertNotNull(dead);
        assertEquals(id, dead.getMessageProperties().getMessageId());
        jdbc.update("UPDATE message_outbox SET state='PENDING',failures=0,publish_token=NULL,next_attempt_at=CURRENT_TIMESTAMP WHERE event_id=? AND state='DEAD'", id);
        listeners.start(); publisher.publishDue(); awaitDone(id);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class));
    }

    @Test void imageEventRollsBackWithBusinessTransaction() {
        String uuid = UUID.randomUUID().toString();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            insertImage(uuid);
            outbox.image(uuid, "images/" + uuid + "/original.png");
            throw new IllegalStateException("rollback");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM images", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox", Integer.class));
    }

    @Test void imageGeneratesVariantsOnceAndDeletedImageIsNotRestored() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String key = "images/" + uuid + "/original.png";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB), "png", bytes);
        storage.upload("images", key, bytes.toByteArray(), "image/png");
        new TransactionTemplate(transactions).executeWithoutResult(status -> { insertImage(uuid); outbox.image(uuid, key); });
        String id = jdbc.queryForObject("SELECT event_id FROM message_outbox", String.class);
        publisher.publishDue();
        String dispatch = token(id);
        processor.process(id, dispatch, "image"); processor.process(id, dispatch, "image");
        var row = jdbc.queryForMap("SELECT medium_key,thumb_key,visibility FROM images WHERE uuid=?", uuid);
        assertEquals("PRIVATE", row.get("visibility"));
        assertEquals(1200, ImageIO.read(new java.io.ByteArrayInputStream(storage.download("images", (String) row.get("medium_key")))).getWidth());
        assertEquals(400, ImageIO.read(new java.io.ByteArrayInputStream(storage.download("images", (String) row.get("thumb_key")))).getWidth());
        jdbc.update("DELETE FROM images WHERE uuid=?", uuid);
        new TransactionTemplate(transactions).executeWithoutResult(status -> outbox.image(uuid, key));
        String deletedId = jdbc.queryForObject("SELECT event_id FROM message_outbox WHERE state='PENDING' AND event_type='IMAGE_VARIANTS'", String.class);
        publisher.publishDue(); processor.process(deletedId, token(deletedId), "image");
        assertEquals("DONE", state(deletedId));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM images", Integer.class));
    }

    private void insertImage(String uuid) {
        jdbc.update("""
            INSERT INTO images(uuid,user_id,image_name,image_type,image_path,original_key,storage_key,original_ext,original_content_type,visibility,media_version)
            VALUES (?,1,'test.png','PNG','',?,?,'png','image/png','PRIVATE',1)
            """, uuid, "images/" + uuid + "/original.png", "images/" + uuid + "/original.png");
    }

    @Test void cacheFailureRemainsRetryableAfterImageCommit() throws Exception {
        String uuid = UUID.randomUUID().toString();
        new TransactionTemplate(transactions).executeWithoutResult(status -> { insertImage(uuid); outbox.invalidateImage(uuid); });
        String id = jdbc.queryForObject("SELECT event_id FROM message_outbox", String.class);
        publisher.publishDue();
        doThrow(new IllegalStateException("cache unavailable")).when(cache).evict(anyString());
        assertThrows(IllegalStateException.class, () -> processor.process(id, token(id), "image"));
        assertEquals("SENT", state(id));
        failures.failed(id, token(id), new IllegalStateException());
        reset(cache); due(id); publisher.publishDue();
        processor.process(id, token(id), "image");
        assertEquals("DONE", state(id));
        verify(cache).evict(startsWith("image:entity:"));
    }

    @Test void failedVariantWriteRollsBackKeysAndDurablySchedulesCleanup() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String key = "images/" + uuid + "/original.png";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", bytes);
        storage.upload("images", key, bytes.toByteArray(), "image/png");
        new TransactionTemplate(transactions).executeWithoutResult(status -> { insertImage(uuid); outbox.image(uuid, key); });
        String id = jdbc.queryForObject("SELECT event_id FROM message_outbox", String.class);
        publisher.publishDue();
        doThrow(new IllegalStateException("injected storage failure")).when(storage)
                .upload(eq("images"), contains("/thumb."), any(byte[].class), anyString(), anyString());
        assertThrows(IllegalStateException.class, () -> processor.process(id, token(id), "image"));
        assertNull(jdbc.queryForObject("SELECT medium_key FROM images WHERE uuid=?", String.class, uuid));
        String cleanup = jdbc.queryForObject("SELECT event_id FROM message_outbox WHERE event_type='IMAGE_CLEANUP'", String.class);
        publisher.publishDue(); processor.process(cleanup, token(cleanup), "image");
        assertFalse(storage.objectExists("images", "images/" + uuid + "/medium.jpg"));
        assertTrue(storage.objectExists("images", key));
    }

    @Test void lockedImageSerializesConcurrentVisibilityChange() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String key = "images/" + uuid + "/original.png";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", bytes);
        storage.upload("images", key, bytes.toByteArray(), "image/png");
        new TransactionTemplate(transactions).executeWithoutResult(status -> { insertImage(uuid); outbox.image(uuid, key); });
        String id = jdbc.queryForObject("SELECT event_id FROM message_outbox", String.class);
        publisher.publishDue();
        CountDownLatch uploading = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(call -> {
            uploading.countDown(); assertTrue(release.await(10, TimeUnit.SECONDS)); return call.callRealMethod();
        }).when(storage).upload(eq("images"), contains("/medium."), any(byte[].class), anyString(), eq("no-store"));
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> consumer = pool.submit(() -> { try { processor.process(id, token(id), "image"); }
                catch (Exception e) { throw new RuntimeException(e); } });
            assertTrue(uploading.await(10, TimeUnit.SECONDS));
            CountDownLatch updating = new CountDownLatch(1);
            Future<?> editor = pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                updating.countDown();
                jdbc.queryForMap("SELECT * FROM images WHERE uuid=? FOR UPDATE", uuid);
                jdbc.update("UPDATE images SET visibility='SPECIFIED',visible_usernames='friend',image_name='edited.png' WHERE uuid=?", uuid);
            }));
            assertTrue(updating.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> editor.get(200, TimeUnit.MILLISECONDS));
            release.countDown(); consumer.get(10, TimeUnit.SECONDS); editor.get(10, TimeUnit.SECONDS);
        } finally { release.countDown(); }
        var row = jdbc.queryForMap("SELECT visibility,image_name,thumb_key FROM images WHERE uuid=?", uuid);
        assertEquals("SPECIFIED", row.get("visibility")); assertEquals("edited.png", row.get("image_name"));
        assertNotNull(row.get("thumb_key"));
    }

    @Test void v4UpgradePreservesLegacyAuditAndSecondStartupIsNoop() {
        String db = "mq_migration_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13316/" + db + "?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false";
        String password = System.getenv("MQ_TEST_PASSWORD");
        var old = org.flywaydb.core.Flyway.configure().dataSource(url, "root", password)
                .locations("classpath:db/migration/versioned").target("4").load();
        old.migrate();
        var legacy = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(url, "root", password));
        legacy.update("INSERT INTO audit_log(action,module,result) VALUES ('LEGACY','TEST','SUCCESS')");
        var current = org.flywaydb.core.Flyway.configure().dataSource(url, "root", password)
                .locations("classpath:db/migration/versioned").load();
        assertEquals(1, current.migrate().migrationsExecuted);
        assertEquals(0, current.migrate().migrationsExecuted);
        assertEquals(1, legacy.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_id IS NULL", Integer.class));
        assertEquals(0, legacy.queryForObject("SELECT COUNT(*) FROM message_outbox", Integer.class));
        jdbc.execute("DROP DATABASE " + db);
    }
    private void awaitDone(String id) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!"DONE".equals(state(id)) && System.nanoTime() < deadline) Thread.sleep(100);
        assertEquals("DONE", state(id));
    }
}
