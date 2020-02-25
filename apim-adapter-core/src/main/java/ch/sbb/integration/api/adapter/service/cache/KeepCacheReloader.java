package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.model.ConfigType;
import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

public class KeepCacheReloader  <K, V> implements CacheLoader<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceToMetricsCache.class);
    private final BiFunction<K, Boolean, V> loadValueFunction;
    private final ConfigType configType;

    public KeepCacheReloader(BiFunction<K, Boolean, V> loadValueFunction, ConfigType configType) {
        this.loadValueFunction = loadValueFunction;
        this.configType = configType;
    }

    @Override
    public V load(K key) {
        return loadValueFunction.apply(key, true);
    }

    @Override
    public V reload(K key, V oldValue) {
        try {
            return loadValueFunction.apply(key, false);
        } catch (Exception e) {
            LOG.warn(ReasonCode.APIM_2008.pattern(), configType.getId(), oldValue, e);
            return oldValue;
        }
    }
}
