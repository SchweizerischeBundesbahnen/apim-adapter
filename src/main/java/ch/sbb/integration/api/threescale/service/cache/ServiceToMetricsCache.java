package ch.sbb.integration.api.threescale.service.cache;

import ch.sbb.integration.api.threescale.config.RestConfig;
import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import ch.sbb.integration.api.threescale.model.MappingRule;
import ch.sbb.integration.api.threescale.model.Metric;
import ch.sbb.integration.api.threescale.service.exception.ThreeScaleAdapterException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ServiceToMetricsCache {

    private static final Logger LOG = Logger.getLogger(ServiceToMetricsCache.class);

    private final LoadingCache<String, List<Metric>> serviceToMetrics;
    private final LoadingCache<MetricIdentifier, String> metricToJson;

    private final ResteasyClient rest;

    private final String threeScaleUrl;
    private final String threeScaleAdminUrl;
    private final String adminToken;

    public ServiceToMetricsCache() {
        rest = RestConfig.newRestEasyClient();
        String protocol = ThreeScaleConfig.threeScaleHostUseHttps() ? "https" : "http";
        threeScaleUrl = protocol + "://" + ThreeScaleConfig.threeScaleHost();
        threeScaleAdminUrl = threeScaleUrl + "/admin/api";
        adminToken = ThreeScaleConfig.adminToken();

        // --------------------------------------------------------------
        // Initialize Instance:
        // --------------------------------------------------------------
        serviceToMetrics = Caffeine.newBuilder()
                .maximumSize(10_000)
                .refreshAfterWrite(ThreeScaleConfig.configReloadInSeconds(), TimeUnit.SECONDS)
                .build(serviceId -> loadMetrics(serviceId));
        metricToJson = Caffeine.newBuilder()
                .maximumSize(10_000)
                .refreshAfterWrite(ThreeScaleConfig.configReloadInSeconds(), TimeUnit.SECONDS)
                .build(metricId -> loadMetricFrom3ScaleBackend(metricId));
    }

    public List<Metric> get(String serviceId) {
        return serviceToMetrics.get(serviceId);
    }

    private List<Metric> loadMetrics(String serviceId) throws ThreeScaleAdapterException {
        List<Metric> metrics = new ArrayList<>();

        String mappingRulesUrl = threeScaleAdminUrl + "/services/" + serviceId + "/proxy/mapping_rules.json?access_token=" + adminToken;
        try (Response mappingRulesResponse = rest.target(mappingRulesUrl).request().get()) {
            if (mappingRulesResponse.getStatus() == HttpStatus.SC_OK) {
                String json = mappingRulesResponse.readEntity(String.class);
                try {
                    JsonNode rootNode = new ObjectMapper().readTree(json).get("mapping_rules");
                    List<JsonNode> mappingRulesNode = rootNode.findValues("mapping_rule");
                    for (JsonNode mappingRuleNode : mappingRulesNode) {
                        String metricId = mappingRuleNode.get("metric_id").asText();
                        String mappingRuleId = mappingRuleNode.get("id").asText();
                        String pattern = mappingRuleNode.get("pattern").asText();
                        String httpMethod = mappingRuleNode.get("http_method").asText();

                        Metric metric = createMetricFor(metricId, serviceId);

                        MappingRule mappingRule = new MappingRule();
                        mappingRule.setId(mappingRuleId);
                        mappingRule.setPattern(pattern);
                        mappingRule.setMethod(httpMethod);

                        metric.setMappingRule(mappingRule);

                        LOG.debug(metric);
                        metrics.add(metric);
                    }

                } catch (IOException e) {
                    throw new ThreeScaleAdapterException("Error Reading MappingRules Response", e);
                }
            } else {
                throw new ThreeScaleAdapterException("Unable to load MappingRules (got an " + mappingRulesResponse.getStatus() + ") with URL: " + mappingRulesUrl);
            }
        }

        return metrics;
    }

    private Metric createMetricFor(String metricId, String serviceId) {
        String json = metricToJson.get(new MetricIdentifier(metricId, serviceId));

        try {
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode metricNode = root.get("metric");
            return toMetric(metricNode);
        } catch (IOException e) {
            throw new ThreeScaleAdapterException("Error Reading Metrics Response", e);
        }
    }

    private Metric toMetric(JsonNode metricNode) {
        String id = metricNode.get("id").asText();
        String sysName = metricNode.get("system_name").asText(); // <- this is the important part
        Metric metric = new Metric();
        metric.setId(id);
        metric.setSystem_name(sysName);
        return metric;
    }

    private String loadMetricFrom3ScaleBackend(MetricIdentifier metricIdentifier) {
        String metricId = metricIdentifier.metricId;
        String serviceId = metricIdentifier.serviceId;
        String metricUrl = threeScaleAdminUrl + "/services/" + serviceId + "/metrics/" + metricId + ".json?access_token=" + adminToken;
        LOG.debug("MetricUrl: " + metricUrl);
        try (Response metricsResponse = rest.target(metricUrl).request().get()) {
            if (metricsResponse.getStatus() == HttpStatus.SC_OK) {
                return metricsResponse.readEntity(String.class);
            } else {
                throw new ThreeScaleAdapterException("Unable to load Metrics (got an " + metricsResponse.getStatus() + ") with URL: " + metricUrl);
            }
        }
    }

    protected static final class MetricIdentifier {
        protected final String metricId;
        protected final String serviceId;

        public MetricIdentifier(String metricId, String serviceId) {
            this.metricId = metricId;
            this.serviceId = serviceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MetricIdentifier that = (MetricIdentifier) o;

            if (!metricId.equals(that.metricId)) return false;
            return serviceId.equals(that.serviceId);
        }

        @Override
        public int hashCode() {
            int result = metricId.hashCode();
            result = 31 * result + serviceId.hashCode();
            return result;
        }
    }

}
