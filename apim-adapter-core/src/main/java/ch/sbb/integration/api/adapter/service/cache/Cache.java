package ch.sbb.integration.api.adapter.service.cache;

public interface Cache<T> {

    T get(String key);

    long size();

    com.github.benmanes.caffeine.cache.Cache<?, ?> get();
}
