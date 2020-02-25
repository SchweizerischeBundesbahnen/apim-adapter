package ch.sbb.integration.api.adapter.service;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.config.util.check.CacheCheck;
import ch.sbb.integration.api.adapter.config.util.check.ConnectionCheck;
import ch.sbb.integration.api.adapter.config.util.check.PublicKeyCheck;
import ch.sbb.integration.api.adapter.config.util.check.SyncCheck;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.model.Metric;
import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.model.reporting.Hits;
import ch.sbb.integration.api.adapter.model.reporting.ResponseSummary;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.service.apiwatch.ApiWatch;
import ch.sbb.integration.api.adapter.service.cache.*;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.configuration.OperationMode;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleScheduler;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleSynchronizerService;
import ch.sbb.integration.api.adapter.service.monitoring.MonitoringService;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleAdminCommunicationComponent;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import ch.sbb.integration.api.adapter.service.utils.ErrorReason;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;
import static ch.sbb.integration.api.adapter.service.monitoring.MonitoringService.TERMINATION_TIMEOUT;
import static ch.sbb.integration.api.adapter.service.utils.ErrorReason.*;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Created by u217269 on 15.02.2018.
 * <p>
 * Refactor: extract caches and batch-runner to separate classes,
 * maybe a separate class for each cache containing all the cache-specific logic?
 */
public class ApimAdapterService implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ApimAdapterService.class);

    private final ThreeScaleScheduler scheduler;

    private final ServiceToMetricsCache serviceToMetricsCache;
    private final ClientCache clientCache;
    private final Cache<Proxy> serviceToProxyCache;
    private final TokenToParsedTokenCache tokenToParsedTokenCache;

    private final ApimAdapterConfig apimAdapterConfig;
    private final MonitoringService monitoringService;
    private final ConnectionCheck connectionCheck;
    private final PublicKeyCheck publicKeyCheck;
    private final SyncCheck syncCheck;

    private final ApiWatch apiWatch;

    private final EmergencyModeState emergencyModeState;

    private final ErrorResponseHelper errorResponseHelper;
    private final Hits hits;

    private final ConfigurationLoader configurationLoader;

    public ApimAdapterService(
            ApimAdapterConfig adapterConfig,
            MonitoringService monitoringService,
            ConnectionCheck connectionCheck,
            PublicKeyCheck publicKeyCheck,
            SyncCheck syncCheck,
            RestConfig restConfig,
            ErrorResponseHelper errorResponseHelper,
            OfflineConfigurationCacheRepo offlineConfigurationCacheRepo,
            EmergencyModeState emergencyModeState,
            OperationMode operationMode) {

        this.apimAdapterConfig = adapterConfig;
        this.monitoringService = monitoringService;
        this.connectionCheck = connectionCheck;
        this.publicKeyCheck = publicKeyCheck;
        this.syncCheck = syncCheck;
        this.emergencyModeState = emergencyModeState;

        this.errorResponseHelper = errorResponseHelper;

        // ---------------------------
        // initialize http clients
        // ---------------------------
        ThreeScaleAdminCommunicationComponent threeScaleAdminCommunicationComponent = new ThreeScaleAdminCommunicationComponent(adapterConfig, restConfig);
        ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent = new ThreeScaleBackendCommunicationComponent(adapterConfig, restConfig);
        configurationLoader = new ConfigurationLoader(offlineConfigurationCacheRepo, emergencyModeState, threeScaleAdminCommunicationComponent, threeScaleBackendCommunicationComponent);


        if (adapterConfig.isApimFilterEnabled()) {
            // ---------------------------
            // initialize caches
            // ---------------------------
            serviceToMetricsCache = new ServiceToMetricsCache(adapterConfig, configurationLoader);
            serviceToProxyCache = new ServiceToProxyCache(adapterConfig, configurationLoader);
            tokenToParsedTokenCache = new TokenToParsedTokenCache(adapterConfig, restConfig, offlineConfigurationCacheRepo);
            clientCache = new ClientCache(serviceToMetricsCache::get, adapterConfig, configurationLoader);

            monitoringService.addCache("serviceToMetrics", serviceToMetricsCache);
            monitoringService.addCache("serviceToProxy", serviceToProxyCache);
            monitoringService.addCache("tokenToClientId", tokenToParsedTokenCache);
            monitoringService.addCache("clientIdToUsage", clientCache);

            // ---------------------------
            // initialize scheduler
            // ---------------------------
            hits = new Hits();

            ThreeScaleSynchronizerService threeScaleSynchronizer = new ThreeScaleSynchronizerService(
                    clientCache,
                    serviceToMetricsCache,
                    adapterConfig.getAdapterServiceId(),
                    configurationLoader,
                    threeScaleBackendCommunicationComponent,
                    emergencyModeState,
                    hits);
            scheduler = new ThreeScaleScheduler(threeScaleSynchronizer);
            scheduler.scheduleSynchronizationOf3ScaleStats(adapterConfig.getAdapterSyncRateInSeconds());
            monitoringService.initializeSyncCollector(scheduler, emergencyModeState);
            apiWatch = new ApiWatch(adapterConfig, offlineConfigurationCacheRepo, tokenToParsedTokenCache, operationMode);
        } else {
            apiWatch = null;
            serviceToMetricsCache = null;
            clientCache = null;
            serviceToProxyCache = null;
            tokenToParsedTokenCache = null;
            scheduler = null;
            hits = null;
        }
    }

    @Override
    public void close() {
        LOG.info(APIM_1026.pattern());
        if (scheduler.isTerminated()) {
            LOG.info("scheduler already terminated");
        } else {
            Future future = triggerSynchronization();
            try {
                future.get(TERMINATION_TIMEOUT, SECONDS);
                LOG.info(APIM_1004.pattern());
                LOG.info("Syncstatus: {}", scheduler.getSyncStatus());
                scheduler.terminate();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error(APIM_3017.pattern(), e);
            }
        }
        try {
            monitoringService.close();
        } catch (Exception e) {
            LOG.warn(APIM_2032.pattern(), e);
        }
    }

    public Future triggerSynchronization() {
        return scheduler.triggerSynchronization();
    }

    // used in apim-gateway
    public Proxy getApiProxy() throws ThreeScaleAdapterException {
        return serviceToProxyCache.get(apimAdapterConfig.getAdapterServiceId());
    }


    public Client readCurrentStats(String clientId) {
        return clientCache.get(clientId);
    }

    /**
     * @see ApimAdapterService#authRep(String, String, String, HttpMethod) but called with a queryString null
     */
    public AuthRepResponse authRep(String token, String path, HttpMethod method) {
        return authRep(token, path, null, method);
    }


    /**
     * This method validates the token, checks the authorisation and increases the hit counter of the api (only if the
     * api has a limit of request)
     *
     * @param token       JWT Token of the user
     * @param path        the location of the request (e.g. /v1/locations)
     * @param queryString query String of the api (e.g. name=foo&amp;version=bar)
     * @param method      enum of the HTTP method
     * @return AuthRepResponse the response object with the http status code, message, clientId and a allowed flag
     * @see AuthRepResponse
     * @see ErrorReason
     */
    public AuthRepResponse authRep(String token, String path, String queryString, HttpMethod method) {
        StopWatch sw = new StopWatch().start();
        if (token == null) {
            return errorResponseHelper.createErrorAuthResponse(null, null, UNAUTHORIZED, path, queryString, method, emptyList());
        }

        final OAuthToken parsedToken = parseToken(token);
        if (parsedToken == null || !parsedToken.isValid()) {
            return createErrorAuthResponse(path, queryString, method, parsedToken);
        }
        AuthRepResponse authRepResponse = authRepWithClientId(parsedToken.getTokenIssuer().getRealm(), parsedToken.getClientId(), path, queryString, method);
        LOG.debug("AuthRep finished: duration={} ms, response='{}' ", sw.stop().getMillis(), authRepResponse);

        return authRepResponse;
    }

    private OAuthToken parseToken(String token) {
        final OAuthToken parsedToken = tokenToParsedTokenCache.get(token);
        // in case signature was invalid
        if (parsedToken != null && parsedToken.getTokenState() == OAuthToken.TokenStateEnum.INVALID_SIGNATURE) {
            // try reloading JWKS
            if (parsedToken.getTokenIssuer() != null && parsedToken.getTokenIssuer().reloadJwks()) {
                // if reloaded, invalidate cached tokens and re-resolve itö
                tokenToParsedTokenCache.invalidateCache();
                return tokenToParsedTokenCache.get(token);
            }
        }
        return parsedToken;
    }

    /**
     * @see ApimAdapterService#authRep(String, String, String, HttpMethod) but called with a ClientId instead of a token.
     */
    public AuthRepResponse authRepWithClientId(String realm, String clientId, String path, String queryString, HttpMethod method) {
        if (path == null || method == null) {
            LOG.warn(APIM_2033.pattern(), path, method);
            return errorResponseHelper.createErrorAuthResponse(realm, clientId, BAD_REQUEST, path, queryString, method, emptyList());
        }

        final MetricMatchingResult metricMatchingResult = matchMetric(path, method, queryString);

        if (clientId == null || clientId.isEmpty()) {
            LOG.warn(APIM_2034.pattern());
            return errorResponseHelper.createErrorAuthResponse(realm, clientId, UNAUTHORIZED, path, queryString, method, metricMatchingResult.getMetricSystemNames());
        }

        final Client client = clientCache.get(clientId);

        if (!client.isAppWithPermission()) {
            return errorResponseHelper.createErrorAuthResponse(realm, clientId, CLIENT_ID_HAS_NO_PERMISSION, path, queryString, method, metricMatchingResult.getMetricSystemNames());
        }

        if (metricMatchingResult.getMatchingPathMetrics().isEmpty()) {
            return errorResponseHelper.createErrorAuthResponse(realm, clientId, PATH_NOT_FOUND, path, queryString, method, metricMatchingResult.getMetricSystemNames());
        }

        if (metricMatchingResult.getMetricSystemNames().isEmpty()) {
            return errorResponseHelper.createErrorAuthResponse(realm, clientId, METHOD_NOT_FOUND, path, queryString, method, metricMatchingResult.getMetricSystemNames());
        }

        if (!incrementUsages(client, metricMatchingResult.getMetricSystemNames())) {
            return errorResponseHelper.createErrorAuthResponse(realm, clientId, LIMIT_EXCEEDED, path, queryString, method, metricMatchingResult.getMetricSystemNames());
        }

        // return true, if at least one Metric matches the path & method and ALL of them returned an accessAllowed and the clientId is valid:
        return new AuthRepResponse(true, clientId, SC_OK, metricMatchingResult.getMetricSystemNames(), "Access granted and reporting reported.", path, queryString, method);
    }

    private AuthRepResponse createErrorAuthResponse(String path, String queryString, HttpMethod method, OAuthToken parsedToken) {
        if (parsedToken == null) {
            LOG.warn(APIM_2035.pattern(), TOKEN_VALIDATION_FAILED);
        }
        final MetricMatchingResult metricMatchingResult = matchMetric(path, method, queryString);
        final ErrorReason errorReason = parsedToken == null ? TOKEN_VALIDATION_FAILED : EXPIRED_OR_INVALID;
        return errorResponseHelper.createErrorAuthResponse(parsedToken, errorReason, path, queryString, method, metricMatchingResult.getMetricSystemNames());
    }


    private MetricMatchingResult matchMetric(String path, HttpMethod method, String queryString) {
        final String pathToValidate = buildPath(path, queryString);
        final List<Metric> matchingPathMetrics = findMatchingPaths(pathToValidate);
        final List<Metric> matchingPathAndMethod = findMatchingPathAndMethod(pathToValidate, method, matchingPathMetrics);
        final List<String> metricSysNames = matchingPathAndMethod.stream()
                .map(Metric::getSystemName)
                .distinct()
                .collect(toList());
        return new MetricMatchingResult(pathToValidate, matchingPathMetrics, matchingPathAndMethod, metricSysNames);
    }

    private String buildPath(String path, String queryString) {
        String buildPath = path;

        if (buildPath.isEmpty()) {
            buildPath = "/"; // In 3Scale Root Paths are always '/' and not empty
        }

        if (queryString != null && !queryString.isEmpty()) {
            buildPath = buildPath + "?" + queryString;
        }
        return buildPath;
    }

    public CheckResult healthCheck() {
        CheckResult adapterCheck = new CheckResult("Adapter CheckResult", Status.UP, "");
        adapterCheck.addCheck(CacheCheck.checkProxyCache(serviceToProxyCache, apimAdapterConfig.getAdapterServiceId()));
        adapterCheck.addCheck(syncCheck.syncCheck(scheduler));
        return adapterCheck;
    }

    public CheckResult readinessCheck() {
        CheckResult adapterCheck = healthCheck();
        adapterCheck.addCheck(publicKeyCheck.checkPublicKey());
        return adapterCheck;
    }

    /**
     * @return True if all metrics were successfully hit, false if at least one failed.
     */
    private boolean incrementUsages(Client client, List<String> metricSysNames) {
        boolean allMetricsSuccesfulUsageIncremented = true;

        for (String metricSysName : metricSysNames) {
            Boolean succesfulUsageIncremented = incrementUsage(client, metricSysName);
            if (!succesfulUsageIncremented) {
                //We have to hit all metrics. So we cannot simply return here. If we would do that, the reporting would be off.
                allMetricsSuccesfulUsageIncremented = false;
            }
        }
        return allMetricsSuccesfulUsageIncremented;
    }

    /**
     * @return True if metric is successful hit, false if current usage is exceeded
     */
    private Boolean incrementUsage(Client client, String metricSysName) {
        return client.incrementUsage(metricSysName);
    }

    private List<Metric> findMatchingPaths(String path) {
        List<Metric> metrics = serviceToMetricsCache.get(apimAdapterConfig.getAdapterServiceId());
        return metrics
                .stream()
                .filter(metric -> metric.matchesAnyPattern(path))
                .collect(toList());
    }

    private List<Metric> findMatchingPathAndMethod(String path, HttpMethod method, List<Metric> filteredMetrics) {
        return filteredMetrics
                .stream()
                .filter(metric -> metric.matchesAnyPathAndMethod(path, method))
                .collect(toList());
    }

    public EmergencyModeState getEmergencyModeState() {
        return emergencyModeState;
    }

    public void reportHit(AuthRepResponse authRepResponse, int httpStatus) {
        if ((CLIENT_ERROR.equals(familyOf(httpStatus)) || SERVER_ERROR.equals(familyOf(httpStatus))) && authRepResponse.isAllowed()) {
            LOG.info(APIM_1027.pattern(),
                    httpStatus, authRepResponse.getMethod(), authRepResponse.getPath(), authRepResponse.getQueryString(), authRepResponse.getMatchingMetricSysNames());
        }
        if (!authRepResponse.isAllowed() && ErrorResponseHelper.isNotMonitoringUrl(authRepResponse.getPath())) {
            LOG.debug("Client was not allowed returned: status='{}', method='{}', path='{}', queryString='{}', matchingMetric='{}'",
                    httpStatus, authRepResponse.getMethod(), authRepResponse.getPath(), authRepResponse.getQueryString(), authRepResponse.getMatchingMetricSysNames());
        }

        if (authRepResponse.getClientId() != null) {
            for (String metricSysName : authRepResponse.getMatchingMetricSysNames()) {
                ResponseSummary responseSummary = new ResponseSummary(authRepResponse.getClientId(), httpStatus, metricSysName);
                if (isUnique(authRepResponse, metricSysName)) {
                    hits.addUnreportHits(responseSummary, 1L);
                }
            }
        } else {
            if (ErrorResponseHelper.isNotMonitoringUrl(authRepResponse.getPath())) {
                LOG.debug("Not reporting 1 hit httpStatus={} for metrics {}. Reason: no client id set.", httpStatus, authRepResponse.getMatchingMetricSysNames());
            }
        }
    }

    private boolean isUnique(AuthRepResponse authRepResponse, String metricSysName) {
        // We do not have to report requests on metric "hits" if this request is reported by an other metric already.
        return !"hits".equals(metricSysName) || authRepResponse.getMatchingMetricSysNames().size() == 1;
    }

    public ApiWatch getApiWatch() {
        return apiWatch;
    }

    protected ApimAdapterConfig getApimAdapterConfig() {
        return apimAdapterConfig;
    }

    protected ConfigurationLoader getConfigurationLoader() {
        return configurationLoader;
    }

    private static class MetricMatchingResult {
        private final String pathToValidate;
        private final List<Metric> matchingPathMetrics;
        private final List<Metric> matchingPathAndMethodMetrics;
        private final List<String> matchingPathAndMethodSysNames;

        private MetricMatchingResult(String pathToValidate, List<Metric> matchingPathMetrics, List<Metric> matchingPathAndMethodMetrics, List<String> matchingPathAndMethodSysNames) {
            this.pathToValidate = pathToValidate;
            this.matchingPathMetrics = matchingPathMetrics;
            this.matchingPathAndMethodMetrics = matchingPathAndMethodMetrics;
            this.matchingPathAndMethodSysNames = matchingPathAndMethodSysNames;
        }

        public String getPathToValidate() {
            return pathToValidate;
        }

        public List<Metric> getMatchingPathMetrics() {
            return matchingPathMetrics;
        }

        public List<Metric> getMatchingPathAndMethodMetrics() {
            return matchingPathAndMethodMetrics;
        }

        public List<String> getMetricSystemNames() {
            return matchingPathAndMethodSysNames;
        }
    }
}
