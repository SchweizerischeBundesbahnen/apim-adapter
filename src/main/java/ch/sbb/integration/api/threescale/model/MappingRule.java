package ch.sbb.integration.api.threescale.model;

import ch.sbb.integration.api.threescale.service.utils.HttpMethod;

import java.io.Serializable;

/**
 * Created by u217269 on 16.02.2018.
 */
public class MappingRule implements Serializable {

    private String id;
    private String pattern;
    private HttpMethod method;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern.replaceAll("[{].*[}]", ".*") + ".*";
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = HttpMethod.valueOf(method.toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MappingRule that = (MappingRule) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MappingRule{" +
                "id='" + id + '\'' +
                ", pattern='" + pattern + '\'' +
                ", method='" + method + '\'' +
                '}';
    }

}
