package ch.sbb.integration.api.adapter.service.converter;

import ch.sbb.integration.api.adapter.model.MappingRule;
import ch.sbb.integration.api.adapter.model.Metric;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class MetricConverter {

    private static final Logger LOG = LoggerFactory.getLogger(MetricConverter.class);

    public List<Metric> convert(JsonNode metricsNode, JsonNode mappingRulesNode) {

        List<MappingRule> mappingRules = mappingRulesNode
                .findValues("mapping_rule")
                .stream()
                .map(MappingRule::of)
                .collect(toList());

        return metricsNode.findValues("metric").stream()
                .map(m -> convertToMetric(m, mappingRules))
                .collect(toList());
    }

    private Metric convertToMetric(JsonNode metricNode, List<MappingRule> mappingRules) {
        String id = metricNode.get("id").asText();
        String name = metricNode.get("name").asText();
        String sysName = metricNode.get("system_name").asText(); // <- this is the important part

        List<MappingRule> filteredMappingRules = filterMappingRules(mappingRules, id, sysName);
        Metric metric = new Metric(id, name, sysName, filteredMappingRules);
        LOG.debug("Metric created: {}", metric);
        return metric;
    }

    private List<MappingRule> filterMappingRules(List<MappingRule> mappingRules, String metricId, String metricSysName) {
        if ("hits".equals(metricSysName)) {
            return mappingRules;
        }
        return mappingRules.stream()
                .filter(mr -> mr.getMetricId().equals(metricId))
                .collect(toList());
    }
}
