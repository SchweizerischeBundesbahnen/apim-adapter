package ch.sbb.integration.api.threescale.service;

import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import ch.sbb.integration.api.threescale.model.*;
import ch.sbb.integration.api.threescale.service.cache.ClientIdToUsageCache;
import ch.sbb.integration.api.threescale.service.cache.ServiceToMetricsCache;
import ch.sbb.integration.api.threescale.service.cache.ServiceToProxyCache;
import ch.sbb.integration.api.threescale.service.cache.TokenToParsedTokenCache;
import ch.sbb.integration.api.threescale.service.job.ThreeScaleScheduler;
import ch.sbb.integration.api.threescale.service.job.ThreeScaleSynchronizerService;
import ch.sbb.integration.api.threescale.service.utils.HttpMethod;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by u217269 on 15.02.2018.
 * <p>
 * Refactor: extract caches and batch-runner to separate classes,
 * maybe a separate class for each cache containing all the cache-specific logic?
 */
public class ThreeScaleAdapterService {

    private static final Logger LOG = Logger.getLogger(ThreeScaleAdapterService.class);

    private ThreeScaleScheduler scheduler;

    private final ServiceToMetricsCache serviceToMetricsCache;
    private final ServiceToProxyCache serviceToProxyCache;
    private final TokenToParsedTokenCache tokenToParsedTokenCache;
    private final ClientIdToUsageCache clientIdToUsageCache;

    private final String serviceId;

    public ThreeScaleAdapterService() {

        // ---------------------------
        // initialize instance
        // ---------------------------
        serviceId = ThreeScaleConfig.serviceId();

        // ---------------------------
        // initialize caches
        // ---------------------------
        serviceToMetricsCache = new ServiceToMetricsCache();
        serviceToProxyCache = new ServiceToProxyCache();
        tokenToParsedTokenCache = new TokenToParsedTokenCache();
        clientIdToUsageCache = new ClientIdToUsageCache(serviceToMetricsCache);

        // ---------------------------
        // initialize scheduler
        // ---------------------------
        ThreeScaleSynchronizerService threeScaleSynchronizer = new ThreeScaleSynchronizerService(clientIdToUsageCache, tokenToParsedTokenCache);
        scheduler = new ThreeScaleScheduler(threeScaleSynchronizer);
        scheduler.scheduleSynchronizationOf3ScaleStats(ThreeScaleConfig.syncRateInSeconds());
    }

    public Collection<ClientStats> readCurrentStats() {
        return clientIdToUsageCache
                .asMap()
                .values();
    }

    public Future triggerSynchronization() {
        return scheduler.triggerSynchronization();
    }

    public Proxy getApiProxyTargetUrl(String serviceId) {
        return serviceToProxyCache.get(serviceId);
    }

    public ClientStats readCurrentStats(String clientId) {
        return clientIdToUsageCache.get(clientId);
    }

    public AuthRepResponse authRep(String token, String path, HttpMethod method) {
        if (token == null || path == null || method == null) {
            LOG.error("Preconditions for metricMatching not fulfilled: Token=" + token + " | Path=" + path + " | Method=" + method);
            return null;
        }

        OAuthToken parsedToken = tokenToParsedTokenCache.get(token);
        if (!parsedToken.isValid()) {
            return new AuthRepResponse(parsedToken.getClientId(), false, HttpStatus.SC_FORBIDDEN, "Invalid Access Token.");
        }

        String clientId = parsedToken.getClientId();
        ClientStats clientStats = clientIdToUsageCache.get(clientId);
        List<Metric> matchingMetrics = findMatchingMetrics(path, method);
        boolean accessDenied = matchingMetrics.stream()
                .map(metric -> clientStats
                        .getUsage(metric.getSystem_name())
                        .hitMetric() // this increments the counter and returns if the access is allowed based on the current usage of this metric.
                )
                .filter(accessAllowed -> !accessAllowed) // search for an accessDenied
                .findFirst()
                .isPresent(); // if found -> accessDenied = true
        // return true, if at least one Metric matches the path & method and ALL of them returned an accessDenied:
        return createAuthRepResponse(clientId, path, method, matchingMetrics, accessDenied);
    }

    private AuthRepResponse createAuthRepResponse(String clientId, String path, HttpMethod method, List<Metric> matchingMetrics, boolean accessDenied) {
        boolean mappingRuleExists = matchingMetrics.size() > 0;
        boolean allowed = mappingRuleExists && !accessDenied;
        int httpStatus = allowed ? HttpStatus.SC_OK : HttpStatus.SC_FORBIDDEN;
        String message = "";
        if (allowed) {
            message = "Access granted and hit on all serviceToMetricsCache reported.";
        } else if (!mappingRuleExists) {
            message = "No Mappingrule found for: " + method + " on " + path;
        } else if (accessDenied) {
            message = "Usage limits exceeded!";
        }

        return new AuthRepResponse(clientId, allowed, httpStatus, message);
    }

    private List<Metric> findMatchingMetrics(String path, HttpMethod method) {
        if (path == null || method == null) {
            LOG.error("Preconditions for metricMatching not fulfilled: Path=" + path + " | Method=" + method);
            return null;
        }
        List<Metric> metrics = serviceToMetricsCache.get(serviceId);
        return metrics
                .parallelStream()
                .filter(metric -> {
                    MappingRule mappingRule = metric.getMappingRule();
                    if (method.equals(mappingRule.getMethod())) {
                        String pattern = mappingRule.getPattern();
                        if (path.matches(pattern))
                            return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

}
