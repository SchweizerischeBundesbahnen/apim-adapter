package ch.sbb.integration.api.adapter.springboot.config;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.config.util.check.ConnectionCheck;
import ch.sbb.integration.api.adapter.config.util.check.PublicKeyCheck;
import ch.sbb.integration.api.adapter.config.util.check.SyncCheck;
import ch.sbb.integration.api.adapter.filter.ApimAdapterFilter;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.configuration.OperationMode;
import ch.sbb.integration.api.adapter.service.monitoring.MonitoringService;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.springboot.config.util.check.HealthCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Configuration
@ConditionalOnProperty(name = "apim.adapter.service-id")
public class ApimAdapterAutoConfiguration {
	private final ApimTokenIssuerConfig tokenIssuerConfig;

	@Value("${apim.backend.use-https:true}")
    private boolean backendUseHttps;
	
	@Value("${apim.backend.port}")
    private String backendPort;
	
	@Value("${apim.backend.host}")
    private String backendHost;
	
	@Value("${apim.backend.token}")
    private String backendToken;
	
	@Value("${apim.admin.use-https:true}")
    private boolean adminUseHttps;
	
	@Value("${apim.admin.host}")
    private String adminHost;
	
	@Value("${apim.admin.token}")
    private String adminToken;

	@Value("${apim.adapter.sync-rate-in-seconds:15}")
    private int adapterSyncRateInSeconds;
    
	@Value("${apim.adapter.config-reload-in-seconds:60}")
	private int adapterConfigReloadInSeconds;
    
	@Value("${apim.adapter.service-id}")
	private String adapterServiceId;
    
	@Value("${apim.adapter.production-mode:true}")
	private boolean adapterProductionMode;

    @Value("${apim.monitoring.level:standard}")
	private String monitoringLevel;
    
	@Value("${apim.monitoring.push.host}")
	private String monitoringPushHost;
    
	@Value("${apim.monitoring.push.interval-in-seconds:60}")
	private int monitoringPushIntervalInSeconds;
    
	@Value("${apim.monitoring.push.enabled:true}")
	private boolean monitoringPushEnabled;
    
	@Value("${apim.monitoring.id}")
	private String monitoringId;

	@Value("${apim.monitoring.namespace}")
	private String monitoringNamespace;

	@Value("${apim.cache.location}")
	private String cacheLocation;

	@Value("${apim.adapter.exclude-filter-methods:}")
    private List<String> excludeFilterMethods;

	@Value("${apim.adapter.report-response-code:false}")
	private boolean reportResponseCode;

	@Value("${apim.adapter.filter.enabled:true}")
	private boolean apimFilterEnabled;

	@Autowired
	public ApimAdapterAutoConfiguration(ApimTokenIssuerConfig tokenIssuerConfig) {
		this.tokenIssuerConfig = tokenIssuerConfig;
	}

	@Bean
    public ApimAdapterConfig apimAdapterConfig() {
    	return ApimAdapterConfig.builder()
    			.backendUseHttps(backendUseHttps)
    			.backendPort(backendPort)
    			.backendToken(backendToken)
    			.backendHost(backendHost)
    			.adminUseHttps(adminUseHttps)
    			.adminHost(adminHost)
    			.adminToken(adminToken)
				.tokenIssuers(tokenIssuerConfig.getTokenissuer())
    			.adapterSyncRateInSeconds(adapterSyncRateInSeconds)
    			.adapterConfigReloadInSeconds(adapterConfigReloadInSeconds)
    			.adapterServiceId(adapterServiceId)
    			.adapterProductionMode(adapterProductionMode)
    			.monitoringLevelAsString(monitoringLevel)
    			.monitoringPushHost(monitoringPushHost)
    			.monitoringPushIntervalInSeconds(monitoringPushIntervalInSeconds)
    			.monitoringPushEnabled(monitoringPushEnabled)
    			.monitoringId(monitoringId)
    			.monitoringNamespace(monitoringNamespace)
				.cacheLocation(cacheLocation)
				.reportResponseCode(reportResponseCode)
				.apimFilterEnabled(apimFilterEnabled)
    			.build();
    }

	@Bean
	@ConditionalOnMissingBean
	public ApimAdapterFilter apimAdapterFilter(ApimAdapterService apimAdapterService) {
        return new ApimAdapterFilter(apimAdapterService, excludeFilterMethods.stream().map(HttpMethod::parse).filter(Objects::nonNull).collect(toList()));
	}

    @Bean
    @ConditionalOnMissingBean
    public ApimAdapterService apimAdapterService(
			ApimAdapterConfig config,
			MonitoringService monitoringService,
			ConnectionCheck connectionCheck,
			PublicKeyCheck publicKeyCheck,
			SyncCheck syncCheck,
			RestConfig restConfig,
			ErrorResponseHelper errorResponseHelper,
			OfflineConfigurationCacheRepo offlineConfigurationCacheRepo,
			EmergencyModeState emergencyModeState) {
    	return new ApimAdapterService(
				config, monitoringService, connectionCheck, publicKeyCheck, syncCheck, restConfig, errorResponseHelper, offlineConfigurationCacheRepo, emergencyModeState, OperationMode.ADAPTER_SPRINGBOOT);
    }

	@Bean
	@ConditionalOnMissingBean
	public OfflineConfigurationCacheRepo apimAdapterOfflineConfigurationCacheRepo(ApimAdapterConfig config) {
		return new OfflineConfigurationCacheRepo(config.getCacheLocation());
	}

	@Bean
	@ConditionalOnMissingBean
	public EmergencyModeState apimAdapterEmergencyModeState() {
		return new EmergencyModeState();
	}

	@Bean
    @ConditionalOnMissingBean
    public MonitoringService apimAdapterMonitoringService(ApimAdapterConfig config, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
    	return new MonitoringService(config, offlineConfigurationCacheRepo);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConnectionCheck apimAdapterConnectionCheck(ApimAdapterConfig config, EmergencyModeState emergencyModeState) {
    	return new ConnectionCheck(config, emergencyModeState);
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicKeyCheck apimAdapterPublicKeyCheck(ApimAdapterConfig config, RestConfig restConfig, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
		return new PublicKeyCheck();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SyncCheck apimAdapterSyncCheck(ApimAdapterConfig config) {
    	return new SyncCheck(config);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RestConfig apimAdapterRestConfig() {
    	return new RestConfig();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ErrorResponseHelper apimAdapterErrorResponseHelper(ApimAdapterConfig config) {
    	return new ErrorResponseHelper(config);
    }
    
    @Bean
    @ConditionalOnProperty(
    		name = "adapter.actuator.healthcheck",
    		matchIfMissing = true,
    		havingValue = "")
    public HealthCheck apimAdapterHealthCheck(ApimAdapterService service) {
    	return new HealthCheck(service);
    }
}
