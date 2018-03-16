package ch.sbb.integration.api.threescale.service.cache;

import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import ch.sbb.integration.api.threescale.model.ClientStats;
import ch.sbb.integration.api.threescale.model.Metric;
import ch.sbb.integration.api.threescale.model.MetricUsage;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import threescale.v3.api.*;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ClientIdToUsageCache {

    private final LoadingCache<String, ClientStats> clientIdToUsage;
    private final ServiceToMetricsCache serviceToMetricsCache;

    // TODO: replace library by a _tolerant_ RestEasy client
    private final ServiceApi serviceApi;
    private final String serviceId;
    private final String serviceToken;


    public ClientIdToUsageCache(ServiceToMetricsCache serviceToMetricsCache) {
        this.serviceToMetricsCache = serviceToMetricsCache;

        // --------------------------------------------------------------
        // Initialize Instance:
        // --------------------------------------------------------------
        this.serviceId = ThreeScaleConfig.serviceId();
        this.serviceApi = ThreeScaleConfig.serviceApi();
        this.serviceToken = ThreeScaleConfig.serviceToken();

        clientIdToUsage = Caffeine.newBuilder()
                .maximumSize(10_000)
                .build(clientId -> initializeClientStats(clientId));
    }

    public ClientStats get(String clientId) {
        return clientIdToUsage.get(clientId);
    }

    public ConcurrentMap<String, ClientStats> asMap() {
        return clientIdToUsage.asMap();
    }

    private ClientStats initializeClientStats(String clientId) throws ServerError {
        ParameterMap params = new ParameterMap();
        params.add("app_id", clientId);
        AuthorizeResponse authorizeResponse = serviceApi.oauth_authorize(serviceToken, serviceId, params);

        ClientStats clientStats = new ClientStats(clientId);

        for (Metric metric : serviceToMetricsCache.get(serviceId)) {
            String metricName = metric.getSystem_name();
            UsageReport report = findUsageReportForMetric(authorizeResponse, metricName);
            if (report != null) {
                Long limit = Long.valueOf(report.getMaxValue());
                Long base = Long.valueOf(report.getCurrentValue());
                String periodStart = report.getPeriodStart();
                String periodEnd = report.getPeriodEnd();

                clientStats.putUsage(
                        metricName,
                        MetricUsage.limitedMetric(clientId, metricName, limit, base, periodStart, periodEnd));
            } else {
                MetricUsage metricUsage = MetricUsage.unlimitedMetric(clientId, metricName);
                clientStats.putUsage(metricName, metricUsage);
            }
        }

        return clientStats;
    }

    private UsageReport findUsageReportForMetric(AuthorizeResponse authorizeResponse, String metricName) {
        for (UsageReport usageReport : authorizeResponse.getUsageReports()) {
            if (usageReport.getMetric().equals(metricName))
                return usageReport;
        }
        return null;
    }

}
