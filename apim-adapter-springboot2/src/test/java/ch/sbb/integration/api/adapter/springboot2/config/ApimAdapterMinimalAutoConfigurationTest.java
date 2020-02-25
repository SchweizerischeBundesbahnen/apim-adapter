package ch.sbb.integration.api.adapter.springboot2.config;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.MonitoringLevel;
import io.prometheus.client.CollectorRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ApimAdapterMinimalAutoConfigurationTest.class, ApimTokenIssuerConfig.class})
@EnableAutoConfiguration
@TestPropertySource(properties = {
        "apim.backend.port=443",
        "apim.backend.host=backend-3scale.dev.app.ose.sbb-aws.net",
        "apim.backend.token=<this would be a token>",
        "apim.admin.host=api-management-dev.app.sbb.ch",
        "apim.admin.token=<this would be a token>",
        "apim.tokenissuer[0].url-pattern=https://sso-dev.sbb.ch/auth/realms/(SBB_Public)",
        "apim.adapter.service-id=123",
        "apim.monitoring.push.host=http://pushgateway-monitoring.dev.app.ose.sbb-aws.net:80",
        "apim.monitoring.id=overrideWithEnv",
        "apim.monitoring.namespace=overrideWithEnv",
        "apim.cache.location=DISABLED"
})
public class ApimAdapterMinimalAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void testConfigAndInjectionWorks() {
        // lookup beans in application context - if beans with custom name are found, auto configuration was executed
        String[] beanDefinitionNames = context.getBeanDefinitionNames();

        assertTrue(Arrays.asList(beanDefinitionNames).contains("apimAdapterConfig"));
        assertTrue(Arrays.asList(beanDefinitionNames).contains("apimAdapterFilter"));
        assertTrue(Arrays.asList(beanDefinitionNames).contains("apimAdapterRestConfig"));

        ApimAdapterConfig config = context.getBean("apimAdapterConfig", ApimAdapterConfig.class);
        // explicit set through @TestPropertySource above
        assertEquals("123", config.getAdapterServiceId());

        // default values in spring boot config
        assertEquals(15, config.getAdapterSyncRateInSeconds());
        assertEquals(60, config.getAdapterConfigReloadInSeconds());
        assertEquals(60, config.getMonitoringPushIntervalInSeconds());
        assertEquals(MonitoringLevel.STANDARD, config.getMonitoringLevel());

        assertEquals("https://sso-dev.sbb.ch/auth/realms/(SBB_Public)", config.getTokenIssuers().get(0).getUrlPattern());
    }

    @BeforeClass
    public static void before() {
        // make sure registry is empty before execution, otherwise multiple test executions collide with "Collector already registered"
        CollectorRegistry.defaultRegistry.clear();
    }
}