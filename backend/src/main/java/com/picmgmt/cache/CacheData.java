package com.picmgmt.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CacheData<T> {

    private final T data;
    private final boolean isNull;
    private final long logicExpireNanos;

    @JsonCreator
    public CacheData(
            @JsonProperty("data") T data,
            @JsonProperty("isNull") boolean isNull,
            @JsonProperty("logicExpireNanos") long logicExpireNanos) {
        this.data = data;
        this.isNull = isNull;
        this.logicExpireNanos = logicExpireNanos;
    }

    public static <T> CacheData<T> of(T data, long logicExpireNanos) {
        return new CacheData<>(data, false, logicExpireNanos);
    }

    public static CacheData<?> nullMarker(long logicExpireNanos) {
        return new CacheData<>(null, true, logicExpireNanos);
    }

    public boolean isLogicallyExpired() {
        return System.nanoTime() > logicExpireNanos;
    }
}
