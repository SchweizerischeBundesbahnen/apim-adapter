package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.Metric;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.converter.MetricConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static ch.sbb.integration.api.adapter.model.ConfigType.METRIC;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ServiceToMetricsCache implements Cache<List<Metric>> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceToMetricsCache.class);
    private static final int MAXIMUM_CACHE_SIZE = 10_000;

    private LoadingCache<String, List<Metric>> serviceToMetrics;

    private final MetricConverter metricConverter;
    private final ConfigurationLoader configurationLoader;

    public ServiceToMetricsCache(ApimAdapterConfig config,
                                 ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
        this.metricConverter = new MetricConverter();

        initCache(config);
    }

    private void initCache(ApimAdapterConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (config.isMonitoringNotDisabledOrMinimal()) {
            builder.recordStats();
        }
        builder.maximumSize(MAXIMUM_CACHE_SIZE);
        serviceToMetrics = builder.refreshAfterWrite(config.getAdapterConfigReloadInSeconds(), TimeUnit.SECONDS)
                .build(new KeepCacheReloader<>(this::loadMetrics, METRIC));
    }

    private List<Metric> loadMetrics(String serviceId, boolean loadFromOfflineCache) {
        JsonNode mappingRulesNode = configurationLoader.loadMappingRulesConfig(serviceId, loadFromOfflineCache);
        JsonNode metricsNode = configurationLoader.loadMetricConfig(serviceId, loadFromOfflineCache);
        List<Metric> metrics = metricConverter.convert(metricsNode, mappingRulesNode);
        LOG.debug("loaded the following metrics: '{}'", metrics);
        return metrics;
    }

    @Override
    public List<Metric> get(String serviceId) {
        return serviceToMetrics.get(serviceId);
    }

    @Override
    public long size() {
        return serviceToMetrics.estimatedSize();
    }

    @Override
    public com.github.benmanes.caffeine.cache.Cache<?, ?> get() {
        return serviceToMetrics;
    }

}
