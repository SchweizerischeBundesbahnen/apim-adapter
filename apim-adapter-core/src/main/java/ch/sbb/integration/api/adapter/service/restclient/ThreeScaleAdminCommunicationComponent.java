package ch.sbb.integration.api.adapter.service.restclient;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;

public class ThreeScaleAdminCommunicationComponent {
    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleAdminCommunicationComponent.class);

    private final ResteasyClient rest;
    private final String threeScaleAdminUrl;
    private final String adminToken;

    public ThreeScaleAdminCommunicationComponent(ApimAdapterConfig config, RestConfig restConfig) {
        this.rest = restConfig.newRestEasyClient();
        final String protocol = config.isAdminUseHttps() ? "https" : "http";
        this.threeScaleAdminUrl = String.format("%s://%s/admin/api", protocol, config.getAdminHost());
        this.adminToken = config.getAdminToken();
    }

    public String loadMappingRulesConfig(String serviceId) {
        LOG.debug("loading mapping rules for service id '{}'", serviceId);
        final StopWatch sw = new StopWatch().start();
        final String mappingRulesUrl = String.format("%s/services/%s/proxy/mapping_rules.json?access_token=%s", threeScaleAdminUrl, serviceId, adminToken);
        try (Response response = rest.target(mappingRulesUrl).request().get()) {
            sw.stop();
            if (response.getStatus() == HttpStatus.SC_OK) {
                LOG.debug("loaded mapping rules with status={} duration={} ms", response.getStatus(), sw.getMillis());
                return response.readEntity(String.class);
            } else {
                LOG.warn(APIM_2026.pattern(), response.getStatus(), sw.getMillis());
                throw new ThreeScaleAdapterException(String.format("Unable to load MappingRules (got an HTTP %d) with URL: %s", response.getStatus(), mappingRulesUrl));
            }
        }
    }

    public String loadMetricConfig(String serviceId) {
        LOG.debug("loading metrics for service id '{}'", serviceId);
        final String metricUrl = String.format("%s/services/%s/metrics.json?access_token=%s", threeScaleAdminUrl, serviceId, adminToken);
        try (Response response = rest.target(metricUrl).request().get()) {
            if (response.getStatus() == HttpStatus.SC_OK) {
                return response.readEntity(String.class);
            } else {
                throw new ThreeScaleAdapterException(APIM_3014.format(response.getStatus(), metricUrl));
            }
        }
    }

    public String loadProxyConfig(String serviceId) {
        LOG.debug("loading proxy for service id '{}'", serviceId);
        final String url = String.format("%s/services/%s/proxy/configs/production/latest.json", threeScaleAdminUrl, serviceId);
        final String urlWithAccessToken = url + "?access_token=" + adminToken;

        try (ClientResponse response = (ClientResponse) rest.target(urlWithAccessToken).request().get()) {
            if (response.getStatus() == HttpStatus.SC_OK) {
                final String json = response.readEntity(String.class);

                if (json == null || json.isEmpty()) {
                    LOG.warn(APIM_2027.pattern(), url, response.getStatus(), response.getReasonPhrase());
                } else {
                    return json;
                }
            } else {
                LOG.warn(APIM_2009.pattern(), response.getStatus(), response.getReasonPhrase(), url);
            }
        }
        throw new ThreeScaleAdapterException(APIM_3015.pattern());
    }
}
