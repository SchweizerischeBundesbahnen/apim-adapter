package ch.sbb.integration.api.adapter.springboot2.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

@Configuration
@AutoConfigureBefore(ApimAdapterAutoConfiguration.class)
public class ApimConfigurationSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ApimConfigurationSupport.class);

    private final Map<String, String> outdatedProperties;

    public ApimConfigurationSupport() {
        outdatedProperties = new HashMap<>();
        outdatedProperties.put("backend.useHttps", "apim.backend.use-https");
        outdatedProperties.put("backend.port", "apim.backend.port");
        outdatedProperties.put("backend.host", "apim.backend.host");
        outdatedProperties.put("backend.token", "apim.backend.token");
        outdatedProperties.put("admin.useHttps", "apim.admin.use-https");
        outdatedProperties.put("admin.host", "apim.admin.host");
        outdatedProperties.put("admin.token", "apim.admin.token");
        outdatedProperties.put("sso.host", "apim.sso.host");
        outdatedProperties.put("adapter.syncRateInSeconds", "apim.adapter.sync-rate-in-seconds");
        outdatedProperties.put("adapter.configReloadInSeconds", "apim.adapter.config-reload-in-seconds");
        outdatedProperties.put("adapter.serviceId", "apim.adapter.service-id");
        outdatedProperties.put("adapter.productionMode", "apim.adapter.production-mode");
        outdatedProperties.put("tls.truststore", "apim.tls.truststore");
        outdatedProperties.put("tls.password", "apim.tls.password");
        outdatedProperties.put("monitoring.level", "apim.monitoring.level");
        outdatedProperties.put("monitoring.push.host", "apim.monitoring.push.host");
        outdatedProperties.put("monitoring.push.intervalInSeconds", "apim.monitoring.push.interval-in-seconds");
        outdatedProperties.put("monitoring.push.enabled", "apim.monitoring.push.enabled");
        outdatedProperties.put("monitoring.id", "apim.monitoring.id");
        outdatedProperties.put("monitoring.namespace", "apim.monitoring.namespace");
        outdatedProperties.put("cacheLocation", "apim.cache.location");
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext().getEnvironment();
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps))
                .forEach(this::logOutdatedProps);
    }

    private void logOutdatedProps(EnumerablePropertySource ps) {
        Arrays.stream(ps.getPropertyNames())
                .filter(outdatedProperties::containsKey)
                .forEach(prop -> logOutdatedProp(ps.getName(), prop));
    }

    private void logOutdatedProp(String propertySourceName, String prop) {
        LOG.warn("Property {} found in PropertySource {}. This Property is deprecated and not supported anymore. Please use: {}", prop, propertySourceName, outdatedProperties.get(prop));
    }

}
