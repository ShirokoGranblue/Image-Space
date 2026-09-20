package com.picmgmt.image;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.picmgmt.cache.CacheService;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.messaging.MessageEvent;
import com.picmgmt.messaging.OutboxService;
import com.picmgmt.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class AsyncImageService {
    private final ImageMapper mapper;
    private final StorageService storage;
    private final OutboxService outbox;
    private final CacheService cache;
    private final MediaMetaCacheService mediaCache;

    public Optional<Image> lockById(Long id) { return Optional.ofNullable(mapper.selectForUpdateById(id)); }
    public Optional<Image> lockByUuid(String uuid) { return Optional.ofNullable(mapper.selectForUpdateByUuid(uuid)); }

    public void enqueue(Image image) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) cache.evict("image:entity:" + image.getId());
            }
        });
        outbox.image(image.getUuid(), image.getOriginalKey());
    }

    public void compensateOnRollback(String uuid, List<String> keys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    // Persist cleanup first: a process restart or object-store outage must not lose it.
                    try { outbox.cleanup(uuid, keys); }
                    catch (RuntimeException failure) {
                        log.error("Cannot persist image compensation: image={}, error={}", uuid, failure.getClass().getSimpleName());
                        for (String key : keys) {
                            try { storage.deleteObjectReliably("images", key); }
                            catch (RuntimeException cleanupFailure) {
                                log.error("Image compensation requires operator attention: image={}, error={}", uuid,
                                        cleanupFailure.getClass().getSimpleName());
                            }
                        }
                    }
                }
            }
        });
    }

    public void generate(MessageEvent event) {
        // The same row lock is taken by update/delete, including before any object-store mutation.
        Image image = mapper.selectForUpdateByUuid(event.imageUuid());
        if (image == null || !Objects.equals(image.getOriginalKey(), event.originalKey())) return;
        if (image.getMediumKey() != null && image.getThumbKey() != null) return;
        if ("webp".equalsIgnoreCase(image.getOriginalExt())) return;
        byte[] bytes = storage.download("images", image.getOriginalKey());
        ImageVariant medium = ImageConvertUtil.createDisplayVariant(bytes, image.getOriginalContentType(), image.getOriginalExt(), 1200);
        ImageVariant thumb = ImageConvertUtil.createDisplayVariant(bytes, image.getOriginalContentType(), image.getOriginalExt(), 400);
        if (medium == null || thumb == null) return;
        String prefix = "images/" + image.getUuid();
        String mediumKey = prefix + "/medium." + medium.ext();
        String thumbKey = prefix + "/thumb." + thumb.ext();
        List<String> keys = List.of(mediumKey, thumbKey);
        compensateOnRollback(image.getUuid(), keys);
        String cacheControl = ImageUrlService.cacheControlForVisibility(image.getVisibility());
        storage.upload("images", mediumKey, medium.bytes(), medium.contentType(), cacheControl);
        storage.upload("images", thumbKey, thumb.bytes(), thumb.contentType(), cacheControl);
        mapper.update(null, new LambdaUpdateWrapper<Image>().eq(Image::getId, image.getId())
                .set(Image::getMediumKey, mediumKey).set(Image::getThumbKey, thumbKey));
        outbox.invalidateImage(image.getUuid());
    }

    public void invalidate(String uuid) {
        Image image = mapper.selectForUpdateByUuid(uuid);
        if (image == null) return;
        cache.evict("image:entity:" + image.getId());
        if (image.getMediumKey() != null) mediaCache.evict(image.getMediumKey());
        if (image.getThumbKey() != null) mediaCache.evict(image.getThumbKey());
    }

    public void cleanup(MessageEvent event) {
        Image image = mapper.selectForUpdateByUuid(event.imageUuid());
        List<String> live = new ArrayList<>();
        if (image != null) {
            live.add(image.getStorageKey()); live.add(image.getOriginalKey());
            live.add(image.getMediumKey()); live.add(image.getThumbKey());
        }
        for (String key : event.cleanupKeys()) {
            if (!key.startsWith("images/" + event.imageUuid() + "/") || key.contains("..") || key.contains("\\")) {
                throw new IllegalArgumentException("Invalid cleanup object key");
            }
            if (!live.contains(key)) storage.deleteObjectReliably("images", key);
        }
    }
}
