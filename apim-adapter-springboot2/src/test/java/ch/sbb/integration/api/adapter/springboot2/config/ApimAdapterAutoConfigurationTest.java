package ch.sbb.integration.api.adapter.springboot2.config;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
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
@SpringBootTest(classes = {ApimAdapterAutoConfigurationTest.class, ApimTokenIssuerConfig.class})
@EnableAutoConfiguration
@TestPropertySource(locations = "classpath:test.properties")
public class ApimAdapterAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void testConfigAndInjectionWorks() {
        // lookup beans in application context - if beans with custom name are found, auto configuration was executed
        String[] beanDefinitionNames = context.getBeanDefinitionNames();

        assertTrue(Arrays.asList(beanDefinitionNames).contains("apimAdapterConfig"));
        assertTrue(Arrays.asList(beanDefinitionNames).contains("apimAdapterFilter"));
        assertTrue(Arrays.asList(beanDefinitionNames).contains("apimAdapterRestConfig"));

        assertEquals("42", context.getBean("apimAdapterConfig", ApimAdapterConfig.class).getAdapterServiceId());
    }

    @BeforeClass
    public static void before() {
        // make sure registry is empty before execution, otherwise multiple test executions collide with "Collector already registered"
        CollectorRegistry.defaultRegistry.clear();
    }
}