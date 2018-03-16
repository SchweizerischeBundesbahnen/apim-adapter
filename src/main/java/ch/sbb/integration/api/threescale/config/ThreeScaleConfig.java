package ch.sbb.integration.api.threescale.config;

import ch.sbb.integration.api.threescale.service.exception.ThreeScaleAdapterException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.log4j.Logger;
import threescale.v3.api.ServiceApi;
import threescale.v3.api.impl.ServiceApiDriver;

import java.io.*;

/**
 * Created by u217269 on 16.02.2018.
 */
public class ThreeScaleConfig {

    private static final Logger LOG = Logger.getLogger(ThreeScaleConfig.class);

    private static final String FILENAME_PROPERTY = "threescale.properties.file";

    private static ThreeScaleConfig INSTANCE;
    private final JsonNode propertiesRoot;
    private final String configFile;
    private ServiceApi serviceApi;

    private ThreeScaleConfig() {
        configFile = System.getProperty(FILENAME_PROPERTY);

        try (InputStream in = loadPropertiesAsStream(configFile)) {
            this.propertiesRoot = new ObjectMapper(new YAMLFactory()).readTree(in);
        } catch (IOException e) {
            throw new RuntimeException("Error at loading config from config file: " + configFile, e);
        }

    }

    private InputStream loadPropertiesAsStream(String configFile) {
        if (configFile == null) {
            LOG.warn("Missing environment parameter: " + FILENAME_PROPERTY + " | switching to default file contained in jar-resources: threescale.yml");
            configFile = "/threescale.yml";
        }
        File file = new File(configFile);
        if (file.exists() && !file.isDirectory() && file.isAbsolute()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new ThreeScaleAdapterException("Error on loading properties(" + FILENAME_PROPERTY + ") file: " + configFile, e);
            }
        } else {
            return ThreeScaleConfig.class.getResourceAsStream(configFile);
        }
    }

    protected static ThreeScaleConfig instance() {
        if (INSTANCE == null) {
            INSTANCE = new ThreeScaleConfig();
        }
        return INSTANCE;
    }

    public static ServiceApi serviceApi() {
        if (instance().serviceApi == null) {
            boolean useHttps = Boolean.valueOf(getValueForProperty("backend.useHttps"));
            int port = Integer.valueOf(getValueForProperty("backend.port"));
            instance().serviceApi = ServiceApiDriver.createApi(getValueForProperty("backend.host"), port, useHttps);
        }
        return instance().serviceApi;
    }

    public static Boolean threeScaleHostUseHttps() {
        return Boolean.valueOf(getValueForProperty("admin.useHttps"));
    }

    public static String threeScaleHost() {
        return getValueForProperty("admin.host");
    }

    public static String truststore() {
        return getValueForProperty("tls.truststore");
    }

    public static String serviceToken() {
        return getValueForProperty("backend.token");
    }

    public static String adminToken() {
        return getValueForProperty("admin.token");
    }

    public static String serviceId() {
        return getValueForProperty("adapter.serviceId");
    }

    public static int syncRateInSeconds() {
        return Integer.valueOf(getValueForProperty("adapter.syncRateInSeconds"));
    }

    public static int configReloadInSeconds() {
        return Integer.valueOf(getValueForProperty("adapter.configReloadInSeconds"));
    }

    public static String getValueForProperty(String propertyQualifier) {
        // Environment overrides property from file:
        String environmentVariable = System.getenv(propertyQualifier.toUpperCase().replaceAll("[.]", "_"));
        if (environmentVariable != null) {
            return environmentVariable;
        }
        JsonNode node = instance().propertiesRoot;
        for (String nodeName : propertyQualifier.split("[.]")) {
            node = node.get(nodeName);
            if (node == null) {
                throw new IllegalArgumentException("Error at loading config(" + propertyQualifier + ") from config file: " + instance().configFile + ". Node with Name(" + nodeName + ") does not exist.");
            }
        }
        return node.asText();
    }

}
