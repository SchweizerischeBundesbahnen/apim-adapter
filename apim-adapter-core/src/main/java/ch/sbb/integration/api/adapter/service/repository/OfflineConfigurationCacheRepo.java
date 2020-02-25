package ch.sbb.integration.api.adapter.service.repository;

import ch.sbb.integration.api.adapter.model.ConfigType;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;

public class OfflineConfigurationCacheRepo {
    private static final Logger LOG = LoggerFactory.getLogger(OfflineConfigurationCacheRepo.class);

    /**
     * Timestamp pattern which is used in file names
     */
    private static final DateTimeFormatter FILE_TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyyMMddHHmmss_SSS_");
    static final int NUMBER_ROTATING_FILES = 3;
    static final String CONFIG_DIR_NAME = "config";
    static final String DISABLED = "DISABLED";

    private OfflineConfigurationState state;
    private final Map<ConfigType, Path> dirs = new EnumMap<>(ConfigType.class);

    public static OfflineConfigurationCacheRepo disabled() {
        return new OfflineConfigurationCacheRepo(DISABLED);
    }

    public OfflineConfigurationCacheRepo(final String apimAdapterCacheLocation) {
        if (DISABLED.equalsIgnoreCase(apimAdapterCacheLocation)) {
            this.state = OfflineConfigurationState.DISABLED;
        } else {
            this.state = initDirectories(apimAdapterCacheLocation);
        }
    }

    private OfflineConfigurationState initDirectories(final String apimAdapterCacheLocation) {
        try {
            Path rootDir = Paths.get(apimAdapterCacheLocation);
            LOG.info(APIM_1032.pattern(), rootDir.toAbsolutePath());
            final Path configDir = rootDir.resolve(CONFIG_DIR_NAME);

            for (ConfigType configType : ConfigType.values()) {
                final Path configSubDir = configDir.resolve(configType.getId());
                Files.createDirectories(configSubDir);
                dirs.put(configType, configSubDir);
            }
            return OfflineConfigurationState.ENABLED;
        } catch (Exception e) {
            LOG.error(APIM_3010.pattern(), e);
            return OfflineConfigurationState.INVALID_CONFIG;
        }
    }

    public boolean persistMetricConfig(final String serviceId, final String metricConfig) {
        return persist(ConfigType.METRIC, new String[]{serviceId}, metricConfig, "json");
    }

    public boolean persistProxyConfig(final String serviceId, final String proxyConfig) {
        return persist(ConfigType.PROXY, new String[]{serviceId}, proxyConfig, "json");
    }

    public boolean persistPlanConfig(final String serviceId, final String clientId, final String planConfig) {
        return persist(ConfigType.PLAN, new String[]{serviceId, clientId}, planConfig, "xml");
    }

    public boolean persistMappingRulesConfig(final String serviceId, final String mappingRulesConfig) {
        return persist(ConfigType.MAPPING_RULES, new String[]{serviceId}, mappingRulesConfig, "json");
    }

    public boolean persistOidc(String tokenIssuerUrl, String oidc) {
        return persist(ConfigType.OIDC, new String[]{urlToFileName(tokenIssuerUrl)}, oidc, "json");
    }

    public boolean persistJwks(String jwksUrl, String jwks) {
        return persist(ConfigType.JWKS, new String[]{urlToFileName(jwksUrl)}, jwks, "json");
    }

    static String urlToFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.substring(url.indexOf("://") + 3).replaceAll("[^-_a-zA-Z0-9]", "_");
    }


    public String findMetricConfig(final String serviceId) {
        return findLatest(ConfigType.METRIC, new String[]{serviceId});
    }

    public String findProxyConfig(final String serviceId) {
        return findLatest(ConfigType.PROXY, new String[]{serviceId});
    }

    public String findPlanConfig(final String serviceId, final String clientId) {
        return findLatest(ConfigType.PLAN, new String[]{serviceId, clientId});
    }

    public String findMappingRulesConfig(final String serviceId) {
        return findLatest(ConfigType.MAPPING_RULES, new String[]{serviceId});
    }

    public String findOidc(String tokenIssuerUrl) {
        return findLatest(ConfigType.OIDC, new String[]{urlToFileName(tokenIssuerUrl)});
    }

    public String findJwks(String jwksUrl) {
        return findLatest(ConfigType.JWKS, new String[]{urlToFileName(jwksUrl)});
    }

    private String findLatest(final ConfigType configType, final String[] ids) {
        if (isDisabled()) {
            return null;
        }

        final String baseFileName = configBaseFileName(configType, ids);
        // maxDepth=1 prevents recursion into subdirectory
        try (final Stream<Path> walk = Files.walk(dir(configType), 1)) {
            final Path latestFile = walk.filter(Files::isRegularFile)
                    .filter(byBaseNameFilter(baseFileName))
                    .max(Comparator.comparing(Path::getFileName))
                    .orElseThrow(() -> new ThreeScaleAdapterException(APIM_3012.format(baseFileName)));

            return new String(Files.readAllBytes(latestFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ThreeScaleAdapterException(APIM_3011.format(baseFileName), e);
        }
    }

    private boolean persist(final ConfigType configType, final String[] ids, final String content, final String extension) {
        if (isDisabled()) {
            return true;
        }

        final String baseFileName = configBaseFileName(configType, ids);
        try {
            // Java uses SystemClock underneath LocalDateTime.now() which may only support millisecond precision.
            // Therefore we depend on Javas internal nano time and extract latest 9 digits. Of course this will not be
            // the remaining nanoseconds of the current time but is good enough as a discriminator beyond the millisecond
            // in order to avoid parallel writes with same file name (not 100% guaranteed - if it should hit one time, nothing bad happens).
            final String nanoTime = String.valueOf(System.nanoTime());
            final String nanoFraction = nanoTime.substring(nanoTime.length() - 9);
            final String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_PATTERN) + nanoFraction;

            final Path config = dir(configType).resolve(String.format("%s_%s.%s", baseFileName, timestamp, extension));
            final StopWatch sw = new StopWatch().start();
            Files.write(config, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            if (LOG.isDebugEnabled()) {
                LOG.debug("file={} write took ms={}", config.toFile().getName(), sw.stop().getMillis());
            }

            // after having successfully written the newest file, apply the rotating
            rotate(configType, baseFileName);
            return true;
        } catch (IOException e) {
            LOG.error(APIM_3013.format(baseFileName), e);
            return false;
        }
    }

    public boolean deletePlanConfig(final String serviceId, final String clientId) {
        return deleteAll(ConfigType.PLAN, new String[]{serviceId, clientId});
    }

    private boolean deleteAll(final ConfigType configType, final String[] ids) {
        if (isDisabled()) {
            return true;
        }

        final String baseFileName = configBaseFileName(configType, ids);
        // maxDepth=1 prevents recursion into subdirectory
        try (final Stream<Path> walk = Files.walk(dir(configType), 1)) {
            walk.filter(Files::isRegularFile)
                    .filter(byBaseNameFilter(baseFileName))
                    .forEach(this::silentDelete);
        } catch (IOException e) {
            LOG.warn(APIM_2021.pattern(), baseFileName, e);
        }
        return false;
    }

    Path dir(final ConfigType configType) {
        return dirs.get(configType);
    }

    private void rotate(final ConfigType configType, final String baseFileName) {
        final StopWatch sw = new StopWatch().start();
        // maxDepth=1 prevents recursion into subdirectory
        try (final Stream<Path> walk = Files.walk(dir(configType), 1)) {
            walk.filter(Files::isRegularFile)
                    .filter(byBaseNameFilter(baseFileName))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .skip(NUMBER_ROTATING_FILES)
                    .forEach(this::silentDelete);
            LOG.debug("file rotation took ms={}", sw.stop().getMillis());
        } catch (IOException e) {
            LOG.warn(APIM_2022.pattern(), baseFileName, e);
        }
    }

    private void silentDelete(final Path p) {
        try {
            final StopWatch sw = new StopWatch().start();
            if (Files.deleteIfExists(p)) {
                LOG.debug("Delete successful file={} took ms={}", p, sw.stop().getMillis());
            } else {
                LOG.info("Delete unsuccessful as file no longer exists file={}", p);
            }
        } catch (Exception e) {
            LOG.warn(APIM_2023.pattern(), p, e);
        }
    }

    static Predicate<Path> byBaseNameFilter(final String baseFileName) {
        return file -> {
            final String fileName = file.getFileName().toString();
            return fileName.startsWith(baseFileName);
        };
    }

    static String configBaseFileName(final ConfigType configType, final String[] ids) {
        if (configType.isAppendBasename()) {
            return configType.getId() + "_" + String.join("_", Arrays.asList(ids));
        } else {
            return String.join("_", Arrays.asList(ids));
        }
    }

    private boolean isDisabled() {
        return state == OfflineConfigurationState.DISABLED || state == OfflineConfigurationState.INVALID_CONFIG;
    }

    public OfflineConfigurationState getState() {
        return state;
    }

}
