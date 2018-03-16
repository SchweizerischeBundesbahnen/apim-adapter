package ch.sbb.integration.api.threescale.model;

import java.io.Serializable;

/**
 * Created by u217269 on 16.02.2018.
 */
public class Metric implements Serializable {

    private String id;
    private String system_name;
    private MappingRule mappingRule;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSystem_name() {
        return system_name;
    }

    public void setSystem_name(String system_name) {
        this.system_name = system_name;
    }

    public MappingRule getMappingRule() {
        return mappingRule;
    }

    public void setMappingRule(MappingRule mappingRule) {
        this.mappingRule = mappingRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Metric metric = (Metric) o;

        return id.equals(metric.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Metric{" +
                "id='" + id + '\'' +
                ", system_name='" + system_name + '\'' +
                ", mappingRule=" + mappingRule +
                '}';
    }

}
