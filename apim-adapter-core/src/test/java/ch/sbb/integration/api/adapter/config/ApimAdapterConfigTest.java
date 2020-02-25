package ch.sbb.integration.api.adapter.config;

import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by u217269 on 23.02.2018.
 */
public class ApimAdapterConfigTest {

    private static final String MONITORING_PUSH_HOST = "APIM_MONITORING_PUSH_HOST";

    private static ApimAdapterConfig apimAdapterConfig;
    private static RestConfig restConfig;
    
    @BeforeClass
    public static void init() {
    	System.setProperty("threescale.properties.file", "/threescale-junit.yml");
    	restConfig = new RestConfig();
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testLoadSomeConfigs() {
        int syncRateInSeconds = apimAdapterConfig.getAdapterSyncRateInSeconds();
        assertThat(syncRateInSeconds, is(not(0)));

        String adminToken = apimAdapterConfig.getAdminToken();
        assertNotNull(adminToken);

        ThreeScaleBackendCommunicationComponent threeScaleCommunicationComonent = new ThreeScaleBackendCommunicationComponent(apimAdapterConfig, restConfig);
        assertNotNull(threeScaleCommunicationComonent);
    }

    @Test
    public void testPropertyOverrideByEnvironmentVariable() {
        String givenValue = "http://new-push.ch";
        environmentVariables.set(MONITORING_PUSH_HOST, givenValue);
        // test factory method
        apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        String actualValue = apimAdapterConfig.getMonitoringPushHost();
        assertEquals(givenValue, actualValue);
    }

    @Test
    public void testSystemPropertyOverridesEnvironmentVariable() {
        String givenEnvValue = "http://new-push.ch";
        environmentVariables.set(MONITORING_PUSH_HOST, givenEnvValue);

        String givenPropertyValue = "http://another-new-push.ch";
        System.setProperty(MONITORING_PUSH_HOST, givenPropertyValue);

        // test factory method
        apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        String actualValue = apimAdapterConfig.getMonitoringPushHost();

        assertEquals(givenPropertyValue, actualValue);
    }

    @Test
    public void testIgnoreOldProperties() {
        String givenEnvValue = "/mytruststore.jks";
        environmentVariables.set("TLS_TRUSTSTORE", givenEnvValue);

        String givenPropertyValue = "my-other-truststore.jks";
        environmentVariables.set("TLS_TRUSTSTORE", givenEnvValue);

        // test factory method
        apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
    }
}