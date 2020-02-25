package ch.sbb.integration.api.adapter.factory;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.config.util.ConfigLoader;
import ch.sbb.integration.api.adapter.config.util.check.ConnectionCheck;
import ch.sbb.integration.api.adapter.config.util.check.PublicKeyCheck;
import ch.sbb.integration.api.adapter.config.util.check.SyncCheck;
import ch.sbb.integration.api.adapter.filter.ApimAdapterFilter;
import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.configuration.OperationMode;
import ch.sbb.integration.api.adapter.service.monitoring.MonitoringService;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2007;
import static java.util.stream.Collectors.toList;

public final class ApimAdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ApimAdapterFactory.class);

    private ApimAdapterFactory() {
    }

    public static ApimAdapterService createApimAdapterService() {
        ApimAdapterConfig config = readConfig();
        return createApimAdapterService(config);
    }

    public static ApimAdapterService createApimAdapterService(ApimAdapterConfig config) {
        return createApimAdapterService(config, OperationMode.ADAPTER_JAVA);
    }

    public static ApimAdapterService createApimAdapterService(ApimAdapterConfig config, OperationMode operationMode) {
        return createApimAdapterService(config, operationMode, true);
    }

    /**
     * This method is intended to be directly invoked from APIM components only
     *
     * @param config
     * @param operationMode
     * @param apimFilterEnabled
     * @return
     */
    public static ApimAdapterService createApimAdapterService(ApimAdapterConfig config, OperationMode operationMode, boolean apimFilterEnabled) {
        final EmergencyModeState emergencyModeState = new EmergencyModeState();
        final OfflineConfigurationCacheRepo offlineConfigurationCacheRepo = new OfflineConfigurationCacheRepo(config.getCacheLocation());
        final RestConfig restConfig = new RestConfig();

        if (!apimFilterEnabled) {
            LOG.warn(APIM_2007.pattern());
            config.setApimFilterEnabled(false);
            return new DisabledApimAdapterService(config, restConfig, offlineConfigurationCacheRepo,
                    emergencyModeState, operationMode);
        }

        final MonitoringService monitoringService = new MonitoringService(config, offlineConfigurationCacheRepo);
        final ConnectionCheck connectionCheck = new ConnectionCheck(config, emergencyModeState);
        final SyncCheck syncCheck = new SyncCheck(config);
        final PublicKeyCheck publicKeyCheck = new PublicKeyCheck();
        final ErrorResponseHelper errorResponseHelper = new ErrorResponseHelper(config);

        return new ApimAdapterService(
                config,
                monitoringService,
                connectionCheck,
                publicKeyCheck,
                syncCheck,
                restConfig,
                errorResponseHelper,
                offlineConfigurationCacheRepo,
                emergencyModeState,
                operationMode);
    }

    public static ApimAdapterFilter createApimAdapterFilter() {
        ApimAdapterConfig apimAdapterConfig = readConfig();
        return new ApimAdapterFilter(createApimAdapterService(apimAdapterConfig), apimAdapterConfig.getExcludeFilterMethods());
    }

    public static ApimAdapterConfig createApimAdapterConfig() {
        return readConfig();
    }

    private static ApimAdapterConfig readConfig() {
        final ConfigLoader cl = new ConfigLoader();

        return ApimAdapterConfig.builder()
                .backendUseHttps(Boolean.valueOf(cl.getValueForProperty("apim.backend.use-https")))
                .backendPort(cl.getValueForProperty("apim.backend.port"))
                .backendToken(cl.getValueForProperty("apim.backend.token"))
                .backendHost(cl.getValueForProperty("apim.backend.host"))
                .adminUseHttps(Boolean.valueOf(cl.getValueForProperty("apim.admin.use-https")))
                .adminHost(cl.getValueForProperty("apim.admin.host"))
                .adminToken(cl.getValueForProperty("apim.admin.token"))
                .tokenIssuerUrlPatterns(cl.getValuesForPropertyList("apim.tokenissuer[].url-pattern"))
                .adapterSyncRateInSeconds(Integer.valueOf(cl.getValueForProperty("apim.adapter.sync-rate-in-seconds")))
                .adapterConfigReloadInSeconds(Integer.valueOf(cl.getValueForProperty("apim.adapter.config-reload-in-seconds")))
                .adapterServiceId(cl.getValueForProperty("apim.adapter.service-id"))
                .adapterProductionMode(Boolean.valueOf(cl.getValueForProperty("apim.adapter.production-mode")))
                .monitoringLevelAsString(cl.getValueForProperty("apim.monitoring.level"))
                .monitoringPushHost(cl.getValueForProperty("apim.monitoring.push.host"))
                .monitoringPushIntervalInSeconds(Integer.valueOf(cl.getValueForProperty("apim.monitoring.push.interval-in-seconds")))
                .monitoringPushEnabled(Boolean.valueOf(cl.getValueForProperty("apim.monitoring.push.enabled")))
                .monitoringId(cl.getValueForProperty("apim.monitoring.id"))
                .monitoringNamespace(cl.getValueForProperty("apim.monitoring.namespace"))
                .cacheLocation(cl.getValueForProperty("apim.cache.location"))
                .excludeFilterMethods(cl.getValuesForProperty("apim.adapter.exclude-filter-methods", true).stream().map(HttpMethod::parse).filter(Objects::nonNull).collect(toList()))
                .reportResponseCode(Boolean.valueOf(cl.getValueForProperty("apim.adapter.report-response-code", true)))
                .apimFilterEnabled(true)
                .build();
    }

    private static class DisabledApimAdapterService extends ApimAdapterService {

        public DisabledApimAdapterService(ApimAdapterConfig adapterConfig, RestConfig restConfig, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo, EmergencyModeState emergencyModeState, OperationMode operationMode) {
            super(adapterConfig, null, null, null, null, restConfig, null, offlineConfigurationCacheRepo, emergencyModeState, operationMode);
        }

        @Override
        public void close() {
        }

        @Override
        public Proxy getApiProxy() {
            JsonNode jsonTree = super.getConfigurationLoader().loadProxyConfig(super.getApimAdapterConfig().getAdapterServiceId(), true);
            String targetUrl = jsonTree
                    .get("proxy_config")
                    .get("content")
                    .get("proxy")
                    .get("api_backend").asText();
            return new Proxy(targetUrl);
        }


    }
}
