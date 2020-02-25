package ch.sbb.integration.api.adapter.model;

import ch.sbb.integration.api.adapter.service.utils.HttpMethod;

import java.io.Serializable;
import java.util.List;

/**
 * Created by u217269 on 16.02.2018.
 */

/**
 * This Class represents a combination of a Metric, joined to a mapping rule. It represents a Line of the Mapping Rules Section in the Integration View of an API Definition in 3Scale Admin UI.
 */
public class Metric implements Serializable {

    private static final long serialVersionUID = 5630926647733030334L;

    private final String id;
    private final String name;
    private final String systemName;
    private final List<MappingRule> mappingRule;

    public Metric(String id, String name, String systemName, List<MappingRule> mappingRule) {
        this.id = id;
        this.name = name;
        this.systemName = systemName;
        this.mappingRule = mappingRule;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSystemName() {
        return systemName;
    }

    public List<MappingRule> getMappingRule() {
        return mappingRule;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", systemName='" + systemName + '\'' +
                ", mappingRule=" + mappingRule +
                '}';
    }

    public boolean matchesAnyPattern(String path) {
        return mappingRule.stream()
                .map(MappingRule::getPattern)
                .anyMatch(pattern -> pattern.matcher(path).matches());
    }

    public boolean matchesAnyPathAndMethod(String path, HttpMethod method) {
        return mappingRule.stream()
                .anyMatch(mr -> mr.matches(path, method));
    }
}
