package ch.sbb.integration.api.adapter.model;

import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Created by u217269 on 16.02.2018.
 */
public class MappingRule implements Serializable {

    /**
     * The following chars are escaped.
     */
    private static String[] escapeChars = new String[]{"<", "(", "[", "^", "-", "=", "!", "|", "]", ")", "?", "*", "+", ".", ">"};
    private static final long serialVersionUID = 2995202494602503727L;

    private String id;
    private String metricId;
    private Pattern pattern;
    private HttpMethod method;
    // TODO: consider introducing delta parameter here & in access-logic (would make sense for negative deltas - which is currently not allowed by 3Scale)

    public MappingRule(String id, String metricId, String pattern, String method) {
        this.id = id;
        this.metricId = metricId;
        this.pattern = Pattern.compile(convertToRegexPattern(pattern));
        this.method = HttpMethod.valueOf(method.toUpperCase());
    }

    public String getId() {
        return id;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getMetricId() {
        return metricId;
    }

    /**
     * If there is a pattern "/url/{pattern}/minus-must-be-escaped". The following conversion is executed:
     * 1) Escaped to "/url/{pattern}/minus\-must\-be\-escaped".
     * 2) pattern replacement to "/url/.+/minus\-must\-be\-escaped.*".
     */
    private String convertToRegexPattern(String pattern) {
        String escapedPattern = escape(pattern);

        if (escapedPattern.endsWith("$")) {
            escapedPattern = escapedPattern.substring(0, escapedPattern.length() - 1);
            return escapedPattern.replaceAll("\\{(.*?)\\}", ".+");
        }

        return escapedPattern.replaceAll("\\{(.*?)\\}", ".+") + ".*";
    }

    private String escape(String pattern) {
        for (String escapeChar : escapeChars) {
            String replace = "\\" + escapeChar;
            String with = "\\\\" + escapeChar;
            pattern = pattern.replaceAll(replace, with);
        }
        return pattern;
    }

    public HttpMethod getMethod() {
        return method;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

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

    public static MappingRule of(JsonNode jsonNode) {
        String id = jsonNode.get("id").asText();
        String metricId = jsonNode.get("metric_id").asText();
        String pattern = jsonNode.get("pattern").asText();
        String httpMethod = jsonNode.get("http_method").asText();

        return new MappingRule(id, metricId, pattern, httpMethod);
    }

    public boolean matches(String path, HttpMethod method) {
        return this.method.equals(method) && pattern.matcher(path).matches();
    }
}
