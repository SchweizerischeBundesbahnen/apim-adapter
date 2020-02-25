package ch.sbb.integration.api.adapter.config.util;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;

public class ConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String FILENAME_PROPERTY = "threescale.properties.file";
    private static final String DEFAULT_YML_FILE = "/threescale.yml";
    private static final String ARRAY_NOTATION = "[]";

    private String configFileName;
    private final JsonNode propertiesRoot;

    public ConfigLoader() {
        configFileName = System.getProperty(FILENAME_PROPERTY);
        if (configFileName == null) {
            LOG.warn(APIM_2005.pattern(), FILENAME_PROPERTY, DEFAULT_YML_FILE);
            configFileName = DEFAULT_YML_FILE;
        }
        try (InputStream in = loadPropertiesAsStream()) {
            this.propertiesRoot = new ObjectMapper(new YAMLFactory()).readTree(in);
        } catch (IOException e) {
            throw new ThreeScaleAdapterException(APIM_3019.format(configFileName), e);
        }
    }

    private InputStream loadPropertiesAsStream() {
        final File file = new File(configFileName);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new ThreeScaleAdapterException(APIM_3019.format(configFileName), e);
            }
        } else {
            return ApimAdapterConfig.class.getResourceAsStream(configFileName);
        }
    }

    public String getValueForProperty(String propertyQualifier) {
        return getValueForProperty(propertyQualifier, false);
    }

    public String getValueForProperty(String propertyQualifier, boolean optional) {
        // System Properties overrides Environment. Environment overrides property from file:
        String value = getEnvOrSystemProp(propertyQualifier).orElseGet(() -> singleStringValue(propertyQualifier, getFromConfigFile(propertyQualifier, optional)));
        if (value != null) {
            return value.trim();
        }
        return value;
    }

    public List<String> getValuesForProperty(String propertyQualifier, boolean optional) {
        // System Properties overrides Environment. Environment overrides property from file:
        String s = getEnvOrSystemProp(propertyQualifier).orElseGet(() -> singleStringValue(propertyQualifier, getFromConfigFile(propertyQualifier, optional)));
        return parseAsList(s);
    }

    private String singleStringValue(String propertyQualifier, List<String> values) {
        if (values == null) {
            return null;
        } else if (values.size() == 1) {
            return values.get(0);
        } else {
            throw new IllegalArgumentException(String.format("Error at loading config(%s) from config file: Expected single value but got list of size=%s", propertyQualifier, values.size()));
        }
    }

    public List<String> getValuesForPropertyList(String propertyQualifier) {
        // first try to find overrides from system properties or env variables
        // e.g. "apim.tokenissuer[].url-pattern" -> "apim.tokenissuer.%s.url-pattern"
        final String replacedArrayNotation = propertyQualifier.replaceAll("\\[]\\.", ".%s.");
        if (replacedArrayNotation.contains("%s")) {
            // read up to 20 items from list, unlikely to get config lists with more values...
            final List<String> values = IntStream.range(1, 20).boxed()
                    // apim.tokenissuer.%s.url-pattern -> apim.tokenissuer.1.url-pattern
                    .map(i -> String.format(replacedArrayNotation, i))
                    // apim.tokenissuer.1.url-pattern -> APIM_TOKENISSUER_1_URL_PATTERN
                    .map(ConfigLoader::propertyQualifierToPropertyName)
                    .map(this::getEnvOrSystemProp)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            if (values.size() > 0) {
                return values;
            }
        }

        // if no override found, read from config file
        return getFromConfigFile(propertyQualifier, false);
    }

    private Optional<String> getEnvOrSystemProp(String propertyQualifier) {
        final String propertyName = propertyQualifierToPropertyName(propertyQualifier);
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            return Optional.of(propertyValue);
        }
        final String envValue = System.getenv(propertyName);
        if (envValue != null) {
            return Optional.of(envValue);
        }
        return Optional.empty();
    }

    /**
     * @param propertyQualifier the config property qualifier
     * @param optional          if true, null is returned if property could not be loaded, false will lead to {@link IllegalArgumentException}
     * @return the config string
     */
    private List<String> getFromConfigFile(String propertyQualifier, boolean optional) {
        final String[] nodeNames = propertyQualifier.split("[.]");
        return resolveValues(propertiesRoot, nodeNames, 0, propertyQualifier, optional);
    }

    private List<String> resolveValues(JsonNode parentNode, String[] nodeNames, int idx, String propertyQualifier, boolean optional) {
        final boolean listExpected;
        final String nodeName;
        if (nodeNames[idx].endsWith(ARRAY_NOTATION)) {
            listExpected = true;
            nodeName = nodeNames[idx].substring(0, nodeNames[idx].length() - ARRAY_NOTATION.length());
        } else {
            listExpected = false;
            nodeName = nodeNames[idx];
        }

        final JsonNode node = parentNode.get(nodeName);
        if (node == null) {
            if (optional) {
                return null;
            } else {
                throw new IllegalArgumentException(APIM_3020.format(propertyQualifier, this.configFileName, nodeName));
            }
        }
        if (nodeNames.length - 1 == idx) {
            // last node reached
            if (node instanceof NullNode) {
                // required as NullNode#asText() returns String "null"
                return null;
            } else {
                return Collections.singletonList(node.asText());
            }
        } else {
            final boolean isArrayNode = node instanceof ArrayNode;
            if (isArrayNode && !listExpected) {
                throw new IllegalArgumentException(APIM_3024.format(propertyQualifier, this.configFileName, nodeName));
            } else if (!isArrayNode && listExpected) {
                throw new IllegalArgumentException(APIM_3025.format(propertyQualifier, this.configFileName, nodeName));
            }
            if (isArrayNode) {
                return resolveValuesFromList(nodeNames, idx + 1, propertyQualifier, optional, node);
            } else {
                return resolveValues(node, nodeNames, idx + 1, propertyQualifier, optional);
            }
        }
    }

    private List<String> resolveValuesFromList(String[] nodeNames, int idx, String propertyQualifier, boolean optional, JsonNode node) {
        final List<String> values = new ArrayList<>();
        for (JsonNode arrayChildNode : node) {
            final List<String> childValues = resolveValues(arrayChildNode, nodeNames, idx, propertyQualifier, optional);
            if (childValues != null) {
                values.addAll(childValues);
            }
        }
        return values;
    }

    static List<String> parseAsList(String s) {
        if (s == null || s.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(s.split(","));
    }

    static String propertyQualifierToPropertyName(String propertyQualifier) {
        return propertyQualifier.toUpperCase().replaceAll("[-.]", "_");
    }
}
