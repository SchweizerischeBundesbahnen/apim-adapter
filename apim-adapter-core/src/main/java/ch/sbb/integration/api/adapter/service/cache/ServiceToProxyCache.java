package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

import static ch.sbb.integration.api.adapter.model.ConfigType.PROXY;

/**
 * Created by u217269 on 08.03.2018.
 */
public class ServiceToProxyCache implements Cache<Proxy> {

    private static final int MAXIMUM_CACHE_SIZE = 10_000;

    private LoadingCache<String, Proxy> serviceToProxy;

    private final ConfigurationLoader configurationLoader;

    public ServiceToProxyCache(ApimAdapterConfig config,
                               ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
        initCache(config);
    }

    private void initCache(ApimAdapterConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (config.isMonitoringNotDisabledOrMinimal()) {
            builder.recordStats();
        }

        serviceToProxy = builder
                .maximumSize(MAXIMUM_CACHE_SIZE)
                .refreshAfterWrite(config.getAdapterConfigReloadInSeconds(), TimeUnit.SECONDS)
                .build(new KeepCacheReloader<>(this::loadProxySettings, PROXY));
    }

    private Proxy loadProxySettings(String serviceId, boolean loadFromOfflineCache) {
        JsonNode jsonTree = configurationLoader.loadProxyConfig(serviceId, loadFromOfflineCache);
        String targetUrl = jsonTree
                .get("proxy_config")
                .get("content")
                .get("proxy")
                .get("api_backend").asText();
        return new Proxy(targetUrl);
    }

    @Override
    public Proxy get(String serviceId) {
        return serviceToProxy.get(serviceId);
    }

    @Override
    public long size() {
        return serviceToProxy.estimatedSize();
    }

    @Override
    public com.github.benmanes.caffeine.cache.Cache<?, ?> get() {
        return serviceToProxy;
    }

}
