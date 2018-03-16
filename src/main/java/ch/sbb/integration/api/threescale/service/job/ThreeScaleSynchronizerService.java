package ch.sbb.integration.api.threescale.service.job;

import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import ch.sbb.integration.api.threescale.model.ClientStats;
import ch.sbb.integration.api.threescale.model.MetricUsage;
import ch.sbb.integration.api.threescale.model.OAuthToken;
import ch.sbb.integration.api.threescale.service.cache.ClientIdToUsageCache;
import ch.sbb.integration.api.threescale.service.cache.TokenToParsedTokenCache;
import ch.sbb.integration.api.threescale.service.exception.ThreeScaleAdapterException;
import org.apache.log4j.Logger;
import threescale.v3.api.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ThreeScaleSynchronizerService extends TimerTask {

    private static final Logger LOG = Logger.getLogger(ThreeScaleSynchronizerService.class);

    private final ClientIdToUsageCache clientIdToUsageCache;
    private final TokenToParsedTokenCache tokenToParsedTokenCache;
    private final ServiceApi serviceApi;
    private final String serviceToken;
    private final String serviceId;

    public ThreeScaleSynchronizerService(ClientIdToUsageCache clientIdToUsageCache, TokenToParsedTokenCache tokenToParsedTokenCache) {
        this.clientIdToUsageCache = clientIdToUsageCache;
        this.tokenToParsedTokenCache = tokenToParsedTokenCache;

        // --------------------------------------------------------------
        // Initialize Instance:
        // --------------------------------------------------------------
        this.serviceToken = ThreeScaleConfig.serviceToken();
        this.serviceId = ThreeScaleConfig.serviceId();
        this.serviceApi = ThreeScaleConfig.serviceApi();
    }

    @Override
    public void run() {
        try {
            LOG.debug("BatchSynchronizeStatsJob triggered at " + LocalDateTime.now());
            long start = System.nanoTime();
            clientIdToUsageCache
                    .asMap()
                    .values()
                    .forEach(stats -> {
                                reportTo3Scale(stats);
                                syncWith3Scale(stats.getClientId());
                            }
                    );
            LOG.debug("Batch-Job duration: " + (System.nanoTime() - start + " nanoseconds."));
        } catch (Throwable t) {
            LOG.error("Error in Batch Sync with 3Scale.", t);
        }
    }

    private void reportTo3Scale(ClientStats stats) {
        String clientId = stats.getClientId();

        ParameterMap params = new ParameterMap();
        params.add("app_id", clientId);

        ParameterMap usageParams = getUsageParamsForEachMetricInStats(stats);
        if (usageParams.size() > 0) {
            params.add("usage", usageParams);

            try {
                ReportResponse reportResponse = serviceApi.report(serviceToken, serviceId, params);
                if (reportResponse.success()) {
                    LOG.debug("Reporting to 3Scale for ClientId= " + clientId + " successfully done.");
                } else {
                    LOG.warn("Reporting to 3Scale for ClientId= " + clientId + " gone wrong: " + reportResponse.getErrorCode() + " | " + reportResponse.getErrorMessage());
                }
            } catch (ServerError serverError) {
                putBackUsagesOnCounterStack(stats, usageParams); // kind of a rollback on error -> best-effort
                throw new ThreeScaleAdapterException("ServerError when calling the 3Scale Report API with ClientID: " + clientId, serverError);
            }
        } else {
            LOG.debug("There are no hits to be reported to 3Scale.");
        }
    }

    private void putBackUsagesOnCounterStack(ClientStats stats, ParameterMap usageParams) {
        usageParams.getKeys()
                .forEach(metricName -> stats
                        .getUsage(metricName)
                        .addHitCount(Long.valueOf(usageParams.getStringValue(metricName)))
                );
    }

    private ParameterMap getUsageParamsForEachMetricInStats(ClientStats stats) {
        ParameterMap usageParams = new ParameterMap();
        for (MetricUsage metricUsage : stats.usages()) {
            long currentUsage = metricUsage.resetAndGetCounter();
            String metricName = metricUsage.getMetricName();
            LOG.debug("Current usage from Client(" + stats.getClientId() + ") of Metric(" + metricName + ")=" + currentUsage);
            if (currentUsage > 0) {
                usageParams.add(metricName, String.valueOf(currentUsage));
            }
        }
        return usageParams;
    }

    private void syncWith3Scale(String clientId) {
        Optional<String> someValidToken = tokenToParsedTokenCache.asMap().entrySet().stream()
                .filter(entry -> notNull(entry))
                .filter(entry -> matchesClientId(entry, clientId))
                .filter(entry -> notExpired(entry))
                .map(Map.Entry::getKey)
                .findFirst();

        if (someValidToken.isPresent()) {
            // TODO: verify if there is really no solution around this "issue":
            // we are only able to batch-sync to 3Scale when we still have a valid token

            ParameterMap params = new ParameterMap();
            params.add("app_id", clientId);
            params.add("access_token", someValidToken.get());

            try {
                AuthorizeResponse authorizeResponse = serviceApi.oauth_authorize(serviceToken, serviceId, params);
                String errorCode = authorizeResponse.getErrorCode();
                if (isBlank(errorCode)) {
                    ClientStats stats = clientIdToUsageCache.get(clientId);
                    // TODO fix the sync when metrics & mappings change in 3Scale, maybe better iterate over UsageReports from answer?
                    // FIXME: First Tests have shown that update of plans are not being considered with this solution
                    stats.usages().forEach(usage -> {
                        String metricName = usage.getMetricName();
                        UsageReport report = findUsageReportForMetric(authorizeResponse, metricName);
                        if (report != null) {
                            Long base = Long.valueOf(report.getCurrentValue());
                            Long limit = Long.valueOf(report.getMaxValue());
                            String periodStart = report.getPeriodStart();
                            String periodEnd = report.getPeriodEnd();
                            usage.synchronizePeriod(base, limit, periodStart, periodEnd); // <-- here we synchronize the "base" counter with the usage-value from the 3Scale Backend
                        }
                    });
                    LOG.debug("Successfully authorized (OAuth) with ClientId= " + clientId + " and synchronized usages.");
                    LOG.debug("Authorization for client(" + clientId + ") and plan(" + authorizeResponse.getPlan() + ") " + (authorizeResponse.success() ? "allowed " : "denied: ") + authorizeResponse.getReason());

                } else {
                    LOG.warn("Sync with 3Scale for ClientId= " + clientId + " gone wrong: " + errorCode + " | " + authorizeResponse.getReason());
                }
            } catch (ServerError serverError) {
                LOG.error("Sync with 3Scale for ClientId= " + clientId + " gone wrong.", serverError);
            }
        }
    }

    private boolean notExpired(Map.Entry<String, OAuthToken> entry) {
        return !entry.getValue().isExpired();
    }

    private boolean matchesClientId(Map.Entry<String, OAuthToken> entry, String clientId) {
        return Objects.equals(entry.getValue().getClientId(), clientId);
    }

    private boolean notNull(Map.Entry<String, OAuthToken> entry) {
        return (entry != null) && (entry.getValue() != null);
    }

    private boolean isBlank(String str) {
        return str == null || "".equals(str);
    }

    private UsageReport findUsageReportForMetric(AuthorizeResponse authorizeResponse, String metricName) {
        for (UsageReport usageReport : authorizeResponse.getUsageReports()) {
            if (usageReport.getMetric().equals(metricName))
                return usageReport;
        }
        return null;
    }

}
