package ch.sbb.integration.api.threescale.config;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import threescale.v3.api.ServiceApi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by u217269 on 23.02.2018.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ThreeScaleConfig.class)
public class ThreeScaleConfigTest {

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-junit.yml");
    }

    @Test
    public void testLoadSomeConfigs() {
        int syncRateInSeconds = ThreeScaleConfig.syncRateInSeconds();
        assertNotNull(syncRateInSeconds);

        String adminToken = ThreeScaleConfig.adminToken();
        assertNotNull(adminToken);

        ServiceApi serviceApi = ThreeScaleConfig.serviceApi();
        assertNotNull(serviceApi);
    }

    @Test
    public void testPropertyOverrideByEnvironmentVariable() {
        PowerMockito.mockStatic(System.class);

        String givenValue = "mytruststore.jks";
        Mockito.when(System.getenv("TLS_TRUSTSTORE")).thenReturn(givenValue);
        String actualValue = ThreeScaleConfig.getValueForProperty("tls.truststore");

        PowerMockito.verifyStatic(System.class);
        assertEquals(givenValue, actualValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionOnMissingProperty() {
        ThreeScaleConfig.getValueForProperty("missing.property");
    }

}
