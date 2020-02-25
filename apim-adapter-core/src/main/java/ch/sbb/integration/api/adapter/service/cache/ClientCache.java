package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.Metric;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_3005;
import static ch.sbb.integration.api.adapter.model.usage.MetricUsage.unlimitedMetric;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ClientCache implements Cache<Client> {

    private static final int MAXIMUM_CACHE_SIZE = 10_000;
    private final ConfigurationLoader configurationLoader;
    private final String adapterServiceId;
    private final Function<String, List<Metric>> metricsService;

    private LoadingCache<String, Client> clientCache;

    public ClientCache(
            Function<String, List<Metric>> metricsService,
            ApimAdapterConfig config,
            ConfigurationLoader configurationLoader) {
        this.metricsService = metricsService;
        this.configurationLoader = configurationLoader;

        adapterServiceId = config.getAdapterServiceId();
        initCache(config);
    }

    private void initCache(ApimAdapterConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (config.isMonitoringNotDisabledOrMinimal()) {
            builder.recordStats();
        }
        builder.maximumSize(MAXIMUM_CACHE_SIZE);
        clientCache = builder.build(this::initializeClient);
    }

    public void remove(String clientId) {
        clientCache.invalidate(clientId);
    }

    public List<String> clientIds() {
        return clientCache
                .asMap()
                .values()
                .stream()
                .map(Client::getId)
                .collect(Collectors.toList());
    }

    private Client initializeClient(String clientId) {
        try {
            Client client = configurationLoader.loadPlanConfig(adapterServiceId, clientId, true);
            updateToUnlimitedUsageWhenNoUsageIsDefined(client,adapterServiceId, metricsService);
            return client;
        } catch (Exception e) {
            throw new ThreeScaleAdapterException(APIM_3005.format(adapterServiceId, clientId), e);
        }
    }

    public static void updateToUnlimitedUsageWhenNoUsageIsDefined(Client client, String serviceId, Function<String, List<Metric>> metricsService) {
        for (Metric metric : metricsService.apply(serviceId)) {
            String metricSysName = metric.getSystemName();
            client.updateMetricUsage(metricSysName, existingUsage -> {
                if(existingUsage == null) {
                    return unlimitedMetric(client.getId(), metricSysName);
                }
                return existingUsage;
            });
        }
    }

    @Override
    public Client get(String clientId) {
        return clientCache.get(clientId);
    }

    @Override
    public long size() {
        return clientCache.estimatedSize();
    }

    @Override
    public com.github.benmanes.caffeine.cache.Cache<?, ?> get() {
        return clientCache;
    }
}
