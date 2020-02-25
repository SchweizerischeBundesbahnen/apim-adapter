package ch.sbb.integration.api.adapter.service.monitoring;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.TokenIssuerConfig;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCollector extends Collector {

    private static Logger log = LoggerFactory.getLogger(ConfigCollector.class);

    private List<MetricFamilySamples> mfs = new ArrayList<>();

    public ConfigCollector(ApimAdapterConfig config, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
        log.debug("initializing config collector");
        GaugeMetricFamily gatewayConfig = new GaugeMetricFamily(
                "apim_adapter_configuration",
                "Listed is the config of the Adapter",
                Arrays.asList("serviceId", "trustedTokenIssuerPatterns", "isProductionMode", "threeScaleAdminHost", "backendHost",
                        "configReloadInSeconds", "syncRateInSeconds",
                        "monitoringLevel", "pushIntervalInSeconds", "cacheLocation", "offlineConfigurationCache"));
        final String trustedTokenIssuerPatterns = config.getTokenIssuers().stream().map(TokenIssuerConfig::getUrlPattern).collect(Collectors.joining(";"));
        gatewayConfig.addMetric(
                Arrays.asList(
                        config.getAdapterServiceId(),
                        trustedTokenIssuerPatterns,
                        String.valueOf(config.isAdapterProductionMode()),
                        config.getAdminHost(),
                        config.getBackendHost(),
                        String.valueOf(config.getAdapterConfigReloadInSeconds()),
                        String.valueOf(config.getAdapterSyncRateInSeconds()),
                        config.getMonitoringLevel().toString(),
                        String.valueOf(config.getMonitoringPushIntervalInSeconds()),
                        String.valueOf(config.getCacheLocation()),
                        String.valueOf(offlineConfigurationCacheRepo.getState())),
                1L);
        mfs.add(gatewayConfig);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return mfs;
    }
}
