package ch.sbb.integration.api.adapter.wiremock;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.model.usage.MetricUsage;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.monitoring.MonitoringService;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleAdminCommunicationComponent;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.BeforeClass;
import org.junit.Rule;

import static org.junit.Assert.assertEquals;

/**
 * Created by u217269 on 09.04.2018.
 */
public class AbstractWiremockTest {

    protected static final String CLIENT_ID = "084e8c30";
    protected static final String V1_LOCATIONS = "/v1/locations";
    protected static final String V1_NEW_LOCATIONS = "/v1/betterLocation";
    protected static final String V1_REGEXTEST1 = "/v1/swisspass/11.1/SwissPassService";
    protected static final String V1_REGEXTEST2 = "/v1/test/swisspass/11.1/SwissPassService";
    protected static final String V1_REGEXTEST3 = "/v1/swisspass/11.1/SwissPassService/foobar";
    protected static final String V2_REGEXTEST4 = "/v2/test/swisspass/22.2/SwissPassService";

    public static ApimAdapterConfig apimAdapterConfig;
    public static RestConfig restConfig;
    public static MonitoringService monitoringService;
    public static ErrorResponseHelper errorResponseHelper;
    public static ConfigurationLoader configurationLoader;
    public static ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent;
    public static EmergencyModeState emergencyModeState;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8099);

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-junit.yml");
        apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        restConfig = new RestConfig();
        OfflineConfigurationCacheRepo offlineConfigurationCacheRepo = new OfflineConfigurationCacheRepo(apimAdapterConfig.getCacheLocation());
        monitoringService = new MonitoringService(apimAdapterConfig, offlineConfigurationCacheRepo);
    	errorResponseHelper = new ErrorResponseHelper(apimAdapterConfig);
        emergencyModeState = new EmergencyModeState();
        ThreeScaleAdminCommunicationComponent threeScaleAdminCommunicationComponent = new ThreeScaleAdminCommunicationComponent(apimAdapterConfig, restConfig);
        threeScaleBackendCommunicationComponent = new ThreeScaleBackendCommunicationComponent(apimAdapterConfig, restConfig);

        configurationLoader = new ConfigurationLoader(offlineConfigurationCacheRepo, emergencyModeState, threeScaleAdminCommunicationComponent, threeScaleBackendCommunicationComponent);
    }

    public void assertUsages(Client stats, String metricSysName, Long base, long hitCount) {

		MetricUsage hitsUsage = stats.getUsage(metricSysName);
		assertEquals(metricSysName + "|base", base, hitsUsage.getBase());
		assertEquals(metricSysName + "|hitCount", hitCount, hitsUsage.getCurrentUsage().get());
	}

}
