package ch.sbb.integration.api.adapter.config;

import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2006;
import static java.util.stream.Collectors.toList;

/**
 * Created by u217269 on 16.02.2018.
 */
public class ApimAdapterConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ApimAdapterConfig.class);

    private final boolean backendUseHttps;
    private final String backendPort;
    private final String backendHost;
    private final String backendToken;
    private final boolean adminUseHttps;
    private final String adminHost;
    private final String adminToken;
    private final List<TokenIssuerConfig> tokenIssuers;
    private final int adapterSyncRateInSeconds;
    private final int adapterConfigReloadInSeconds;
    private final String adapterServiceId;
    private final boolean adapterProductionMode;
    private final MonitoringLevel monitoringLevel;
    private final String monitoringPushHost;
    private final int monitoringPushIntervalInSeconds;
    private final boolean monitoringPushEnabled;
    private final String monitoringId;
    private final String monitoringNamespace;
    private final String cacheLocation;
    private final List<HttpMethod> excludeFilterMethods;
    private final boolean reportResponseCode;
    private boolean apimFilterEnabled;

    public ApimAdapterConfig(boolean backendUseHttps, String backendPort, String backendHost, String backendToken,
                             boolean adminUseHttps, String adminHost, String adminToken, List<TokenIssuerConfig> tokenIssuers,
                             int adapterSyncRateInSeconds, int adapterConfigReloadInSeconds, String adapterServiceId,
                             boolean adapterProductionMode, MonitoringLevel monitoringLevel, String monitoringPushHost,
                             int monitoringPushIntervalInSeconds, boolean monitoringPushEnabled, String monitoringId,
                             String monitoringNamespace, String cacheLocation, List<HttpMethod> excludeFilterMethods,
                             boolean reportResponseCode, boolean apimFilterEnabled) {
        this.backendUseHttps = backendUseHttps;
        this.backendPort = backendPort;
        this.backendHost = backendHost;
        this.backendToken = backendToken;
        this.adminUseHttps = adminUseHttps;
        this.adminHost = adminHost;
        this.adminToken = adminToken;
        this.tokenIssuers = tokenIssuers;
        this.adapterSyncRateInSeconds = adapterSyncRateInSeconds;
        this.adapterConfigReloadInSeconds = adapterConfigReloadInSeconds;
        this.adapterServiceId = adapterServiceId;
        this.adapterProductionMode = adapterProductionMode;
        this.monitoringLevel = monitoringLevel;
        this.monitoringPushHost = monitoringPushHost;
        this.monitoringPushIntervalInSeconds = monitoringPushIntervalInSeconds;
        this.monitoringPushEnabled = monitoringPushEnabled;
        this.monitoringId = monitoringId;
        this.monitoringNamespace = monitoringNamespace;
        this.cacheLocation = cacheLocation;
        this.excludeFilterMethods = excludeFilterMethods;
        this.reportResponseCode = reportResponseCode;
        this.apimFilterEnabled = apimFilterEnabled;

        if (tokenIssuers == null) {
            throw new IllegalArgumentException("At least one token issuer url pattern must be provided");
        }
        // fail fast - make sure patterns are compilable and there is at least one pattern
        final List<Pattern> urlPatterns = tokenIssuers.stream().map(TokenIssuerConfig::getUrlPatternCompiled).collect(Collectors.toList());
        if (urlPatterns.isEmpty()) {
            throw new IllegalArgumentException("At least one token issuer url pattern must be provided");
        }
    }

    public static ApimAdapterConfigBuilder builder() {
        return new ApimAdapterConfigBuilder();
    }

    public static class ApimAdapterConfigBuilder {
        private boolean backendUseHttps;
        private String backendPort;
        private String backendHost;
        private String backendToken;
        private boolean adminUseHttps;
        private String adminHost;
        private String adminToken;
        private List<TokenIssuerConfig> tokenIssuers;
        private int adapterSyncRateInSeconds;
        private int adapterConfigReloadInSeconds;
        private String adapterServiceId;
        private boolean adapterProductionMode;
        private MonitoringLevel monitoringLevel;
        private String monitoringPushHost;
        private int monitoringPushIntervalInSeconds;
        private boolean monitoringPushEnabled;
        private String monitoringId;
        private String monitoringNamespace;
        private String cacheLocation;
        private List<HttpMethod> excludeFilterMethods;
        private boolean reportResponseCode;
        private boolean apimFilterEnabled = true;


        ApimAdapterConfigBuilder() {
        }

        public ApimAdapterConfigBuilder monitoringLevelAsString(String monitoringLevel) {
            try {
                return this.monitoringLevel(MonitoringLevel.valueOf(monitoringLevel.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOG.warn(APIM_2006.pattern());
                return this.monitoringLevel(MonitoringLevel.STANDARD);
            }
        }

        public ApimAdapterConfigBuilder backendUseHttps(boolean backendUseHttps) {
            this.backendUseHttps = backendUseHttps;
            return this;
        }

        public ApimAdapterConfigBuilder backendPort(String backendPort) {
            this.backendPort = backendPort;
            return this;
        }

        public ApimAdapterConfigBuilder backendHost(String backendHost) {
            this.backendHost = backendHost;
            return this;
        }

        public ApimAdapterConfigBuilder backendToken(String backendToken) {
            this.backendToken = backendToken;
            return this;
        }

        public ApimAdapterConfigBuilder adminUseHttps(boolean adminUseHttps) {
            this.adminUseHttps = adminUseHttps;
            return this;
        }

        public ApimAdapterConfigBuilder adminHost(String adminHost) {
            this.adminHost = adminHost;
            return this;
        }

        public ApimAdapterConfigBuilder adminToken(String adminToken) {
            this.adminToken = adminToken;
            return this;
        }

        public ApimAdapterConfigBuilder tokenIssuerUrlPatterns(List<String> tokenIssuerUrlPatterns) {
            return this.tokenIssuers(tokenIssuerUrlPatterns.stream().map(TokenIssuerConfig::new).collect(toList()));
        }

        public ApimAdapterConfigBuilder tokenIssuers(List<TokenIssuerConfig> tokenIssuers) {
            this.tokenIssuers = tokenIssuers;
            return this;
        }

        public ApimAdapterConfigBuilder adapterSyncRateInSeconds(int adapterSyncRateInSeconds) {
            this.adapterSyncRateInSeconds = adapterSyncRateInSeconds;
            return this;
        }

        public ApimAdapterConfigBuilder adapterConfigReloadInSeconds(int adapterConfigReloadInSeconds) {
            this.adapterConfigReloadInSeconds = adapterConfigReloadInSeconds;
            return this;
        }

        public ApimAdapterConfigBuilder adapterServiceId(String adapterServiceId) {
            this.adapterServiceId = adapterServiceId;
            return this;
        }

        public ApimAdapterConfigBuilder adapterProductionMode(boolean adapterProductionMode) {
            this.adapterProductionMode = adapterProductionMode;
            return this;
        }

        public ApimAdapterConfigBuilder monitoringLevel(MonitoringLevel monitoringLevel) {
            this.monitoringLevel = monitoringLevel;
            return this;
        }

        public ApimAdapterConfigBuilder monitoringPushHost(String monitoringPushHost) {
            this.monitoringPushHost = monitoringPushHost;
            return this;
        }

        public ApimAdapterConfigBuilder monitoringPushIntervalInSeconds(int monitoringPushIntervalInSeconds) {
            this.monitoringPushIntervalInSeconds = monitoringPushIntervalInSeconds;
            return this;
        }

        public ApimAdapterConfigBuilder monitoringPushEnabled(boolean monitoringPushEnabled) {
            this.monitoringPushEnabled = monitoringPushEnabled;
            return this;
        }

        public ApimAdapterConfigBuilder monitoringId(String monitoringId) {
            this.monitoringId = monitoringId;
            return this;
        }

        public ApimAdapterConfigBuilder monitoringNamespace(String monitoringNamespace) {
            this.monitoringNamespace = monitoringNamespace;
            return this;
        }

        public ApimAdapterConfigBuilder cacheLocation(String cacheLocation) {
            this.cacheLocation = cacheLocation;
            return this;
        }

        public ApimAdapterConfigBuilder excludeFilterMethods(List<HttpMethod> excludeFilterMethods) {
            this.excludeFilterMethods = excludeFilterMethods;
            return this;
        }

        public ApimAdapterConfigBuilder reportResponseCode(boolean reportResponseCode) {
            this.reportResponseCode = reportResponseCode;
            return this;
        }

        public ApimAdapterConfigBuilder apimFilterEnabled(boolean apimFilterEnabled) {
            this.apimFilterEnabled = apimFilterEnabled;
            return this;
        }

        public ApimAdapterConfig build() {
            return new ApimAdapterConfig(backendUseHttps, backendPort, backendHost,
                    backendToken, adminUseHttps, adminHost, adminToken,
                    tokenIssuers, adapterSyncRateInSeconds, adapterConfigReloadInSeconds,
                    adapterServiceId, adapterProductionMode, monitoringLevel, monitoringPushHost,
                    monitoringPushIntervalInSeconds, monitoringPushEnabled,
                    monitoringId, monitoringNamespace, cacheLocation, excludeFilterMethods,
                    reportResponseCode, apimFilterEnabled);
        }

        @Override
        public String toString() {
            return "ApimAdapterConfigBuilder{" +
                    "backendUseHttps=" + backendUseHttps +
                    ", backendPort='" + backendPort + '\'' +
                    ", backendHost='" + backendHost + '\'' +
                    ", adminUseHttps=" + adminUseHttps +
                    ", adminHost='" + adminHost + '\'' +
                    ", tokenIssuers='" + tokenIssuers + '\'' +
                    ", adapterSyncRateInSeconds=" + adapterSyncRateInSeconds +
                    ", adapterConfigReloadInSeconds=" + adapterConfigReloadInSeconds +
                    ", adapterServiceId='" + adapterServiceId + '\'' +
                    ", adapterProductionMode=" + adapterProductionMode +
                    ", monitoringLevel=" + monitoringLevel +
                    ", monitoringPushHost='" + monitoringPushHost + '\'' +
                    ", monitoringPushIntervalInSeconds=" + monitoringPushIntervalInSeconds +
                    ", monitoringPushEnabled=" + monitoringPushEnabled +
                    ", monitoringId='" + monitoringId + '\'' +
                    ", monitoringNamespace='" + monitoringNamespace + '\'' +
                    ", cacheLocation='" + cacheLocation + '\'' +
                    ", excludeFilterMethods='" + excludeFilterMethods + '\'' +
                    ", reportResponseCode='" + reportResponseCode + '\'' +
                    '}';
        }

    }

    public String getBackendUrl() {
        return String.format(
                "http%s://%s:%s", isBackendUseHttps() ? "s" : "", getBackendHost(), getBackendPort());
    }

    public boolean isMonitoringNotDisabledOrMinimal() {
        return (!getMonitoringLevel().equals(MonitoringLevel.MINIMAL) && !getMonitoringLevel().equals(MonitoringLevel.NONE));
    }

    public boolean isMonitoringDisabled() {
        return getMonitoringLevel().equals(MonitoringLevel.NONE);
    }

    public boolean isBackendUseHttps() {
        return backendUseHttps;
    }

    public String getBackendPort() {
        return backendPort;
    }

    public String getBackendHost() {
        return backendHost;
    }

    public String getBackendToken() {
        return backendToken;
    }

    public boolean isAdminUseHttps() {
        return adminUseHttps;
    }

    public String getAdminHost() {
        return adminHost;
    }

    public String getAdminToken() {
        return adminToken;
    }

    public List<TokenIssuerConfig> getTokenIssuers() {
        return tokenIssuers;
    }

    public int getAdapterSyncRateInSeconds() {
        return adapterSyncRateInSeconds;
    }

    public int getAdapterConfigReloadInSeconds() {
        return adapterConfigReloadInSeconds;
    }

    public String getAdapterServiceId() {
        return adapterServiceId;
    }

    public boolean isAdapterProductionMode() {
        return adapterProductionMode;
    }

    public MonitoringLevel getMonitoringLevel() {
        return monitoringLevel;
    }

    public String getMonitoringPushHost() {
        return monitoringPushHost;
    }

    public int getMonitoringPushIntervalInSeconds() {
        return monitoringPushIntervalInSeconds;
    }

    public boolean isMonitoringPushEnabled() {
        return monitoringPushEnabled;
    }

    public String getMonitoringId() {
        return monitoringId;
    }

    public String getMonitoringNamespace() {
        return monitoringNamespace;
    }

    public String getCacheLocation() {
        return cacheLocation;
    }

    public List<HttpMethod> getExcludeFilterMethods() {
        return excludeFilterMethods;
    }

    public boolean isReportResponseCode() {
        return reportResponseCode;
    }

    public boolean isApimFilterEnabled() {
        return apimFilterEnabled;
    }

    public void setApimFilterEnabled(boolean apimFilterEnabled) {
        this.apimFilterEnabled = apimFilterEnabled;
    }

    @Override
    public String toString() {
        return "ApimAdapterConfig{" +
                "backendUseHttps=" + backendUseHttps +
                ", backendPort='" + backendPort + '\'' +
                ", backendHost='" + backendHost + '\'' +
                ", adminUseHttps=" + adminUseHttps +
                ", adminHost='" + adminHost + '\'' +
                ", tokenIssuers='" + tokenIssuers + '\'' +
                ", adapterSyncRateInSeconds=" + adapterSyncRateInSeconds +
                ", adapterConfigReloadInSeconds=" + adapterConfigReloadInSeconds +
                ", adapterServiceId='" + adapterServiceId + '\'' +
                ", adapterProductionMode=" + adapterProductionMode +
                ", monitoringLevel=" + monitoringLevel +
                ", monitoringPushHost='" + monitoringPushHost + '\'' +
                ", monitoringPushIntervalInSeconds=" + monitoringPushIntervalInSeconds +
                ", monitoringPushEnabled=" + monitoringPushEnabled +
                ", monitoringId='" + monitoringId + '\'' +
                ", monitoringNamespace='" + monitoringNamespace + '\'' +
                ", cacheLocation='" + cacheLocation + '\'' +
                ", excludeFilterMethods=" + excludeFilterMethods +
                ", reportResponseCode=" + reportResponseCode +
                '}';
    }
}
