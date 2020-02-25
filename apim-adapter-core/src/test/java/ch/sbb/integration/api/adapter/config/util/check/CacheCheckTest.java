package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.cache.Cache;
import ch.sbb.integration.api.adapter.service.cache.ServiceToProxyCache;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.monitoring.MonitoringService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CacheCheckTest {

    private static final String SERVICE_ID = "SERVICE_ID";
    private Cache<Proxy> proxyCache;

    @Mock
    private ApimAdapterConfig mockedApimAdapterConfig;
    @Mock
    private MonitoringService mockedMonitoringService;
    @Mock
    private ConfigurationLoader mockedConfigurationLoader;


    @Before
    public void setup() {
        when(mockedApimAdapterConfig.getAdapterConfigReloadInSeconds()).thenReturn(1);
        when(mockedApimAdapterConfig.getAdapterServiceId()).thenReturn(SERVICE_ID);
        proxyCache = new ServiceToProxyCache(mockedApimAdapterConfig, mockedConfigurationLoader);
    }

    @Test
    public void checkProxyCache() throws IOException, InterruptedException {
        //Arrange
        JsonNode returnValue = new ObjectMapper().readTree(
                "{\"proxy_config\":" +
                        "{\"content\" :" +
                        "{\"proxy\" :" +
                        "{\"api_backend\":\"backend\"}" +
                        "}" +
                        "}" +
                        "}");
        when(mockedConfigurationLoader.loadProxyConfig(eq(SERVICE_ID), any(Boolean.class)))
                .thenReturn(returnValue);

        //Act
        CheckResult check = CacheCheck.checkProxyCache(proxyCache, SERVICE_ID);

        //Assert
        assertTrue(check.isUp());
        assertEquals(Status.UP, check.getStatus());
    }

    @Test
    public void negativeCheckProxyCache() {
        when(mockedConfigurationLoader.loadProxyConfig(eq("invalidKey"), any(Boolean.class))).thenThrow(new ThreeScaleAdapterException("error"));

        CheckResult check = CacheCheck.checkProxyCache(proxyCache, "invalidKey");

        assertFalse(check.isUp());
        assertEquals(Status.DOWN, check.getStatus());
    }

}