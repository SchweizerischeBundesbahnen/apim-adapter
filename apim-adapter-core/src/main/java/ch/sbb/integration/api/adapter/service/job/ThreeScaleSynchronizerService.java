package ch.sbb.integration.api.adapter.service.job;

import ch.sbb.integration.api.adapter.model.Metric;
import ch.sbb.integration.api.adapter.model.reporting.Hits;
import ch.sbb.integration.api.adapter.model.reporting.ResponseSummary;
import ch.sbb.integration.api.adapter.model.status.SyncStatus;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.model.usage.ClientSyncState;
import ch.sbb.integration.api.adapter.model.usage.MetricUsage;
import ch.sbb.integration.api.adapter.service.cache.ClientCache;
import ch.sbb.integration.api.adapter.service.cache.ServiceToMetricsCache;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;
import static ch.sbb.integration.api.adapter.model.usage.MetricUsage.synchronizePeriods;
import static ch.sbb.integration.api.adapter.model.usage.MetricUsage.unlimitedMetric;
import static ch.sbb.integration.api.adapter.service.cache.ClientCache.updateToUnlimitedUsageWhenNoUsageIsDefined;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ThreeScaleSynchronizerService extends TimerTask {

    //See: https://github.com/3scale/apisonator/blob/a99715d10f5816908bdd4184f8c48663728a4255/lib/3scale/backend/transaction.rb#L5
    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleSynchronizerService.class);
    private final ClientCache clientCache;
    private final ServiceToMetricsCache serviceToMetricsCache;
    private final String serviceId;
    private final ConfigurationLoader configurationLoader;

    private final SyncStatus syncStatus = new SyncStatus();
    private final ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent;
    private EmergencyModeState emergencyModeState;
    private final Hits hits;

    public ThreeScaleSynchronizerService(
            ClientCache clientCache,
            ServiceToMetricsCache serviceToMetricsCache,
            String serviceId,
            ConfigurationLoader configurationLoader,
            ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent,
            EmergencyModeState emergencyModeState,
            Hits hits) {
        this.clientCache = clientCache;
        this.serviceToMetricsCache = serviceToMetricsCache;
        this.serviceId = serviceId;

        this.configurationLoader = configurationLoader;
        this.threeScaleBackendCommunicationComponent = threeScaleBackendCommunicationComponent;
        this.emergencyModeState = emergencyModeState;
        this.hits = hits;
    }

    @Override
    public void run() {
        synchronize();
    }

    private void synchronize() {
        try {
            syncStatus.startSync();

            //Try to report
            boolean reportSuccessful = reportHits();
            syncStatus.setLastRunSuccessful(reportSuccessful);

            if (reportSuccessful || emergencyModeState.isEmergencyMode()) {
                clientCache.clientIds().forEach(this::syncWith3Scale);
            }

            syncStatus.finishSync();
        } catch (Exception e) {
            LOG.error(APIM_3001.pattern(), e);
            syncStatus.setLastRunSuccessful(false);
        }
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    private boolean reportHits() {
        syncStatus.updateLastSyncActivity();
        LOG.debug("Start reporting");

        Map<ResponseSummary, Long> metricsToReport = hits.extractUnreportedHits().entrySet().stream()
                .filter(e -> e.getKey().isWithin24Hours()) //We do not care about reporting which are older than a day, we can remove these.
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (metricsToReport.isEmpty()) {
            return true;
        }
        LOG.debug("NumberOfMetricsToReport={}", metricsToReport.size());

        boolean successfulReported = threeScaleBackendCommunicationComponent.report(metricsToReport);
        if(successfulReported) {
            metricsToReport.forEach(this::logHit);
        }else{
            //Reporting went wrong, add the unreported hits again
            metricsToReport.forEach(this::addUnreportHits);
        }
        return successfulReported;
    }

    private void addUnreportHits(ResponseSummary responseSummary, Long count) {
        LOG.info(APIM_1018.pattern(), responseSummary, count);
        hits.addUnreportHits(responseSummary, count);
    }

    private void logHit(ResponseSummary responseSummary, Long count) {
        LOG.debug("Reporting to 3Scale for {}, count='{}'", responseSummary, count);
    }

    private Client loadClient(String clientId) {
        try {
            return configurationLoader.loadPlanConfig(serviceId, clientId, false);
        } catch (Exception e) {
            throw new ThreeScaleAdapterException(APIM_3009.pattern(), e);
        }
    }

    private synchronized void syncWith3Scale(String clientId) {
        syncStatus.updateLastSyncActivity();

        final Client newStats = loadClient(clientId);

        // do not sync if 3Scale is not available
        if (newStats.getSyncState() == ClientSyncState.SERVER_ERROR) {
            syncStatus.setLastRunSuccessful(false);
            return;
        }

        final Client cachedStats = clientCache.get(clientId);
        cachedStats.setSyncState(newStats.getSyncState());

        if (newStats.getSyncState() == ClientSyncState.APPLICATION_NOT_FOUND) {
            LOG.info(APIM_1019.pattern(), clientId);
            clientCache.remove(clientId);
            LOG.info(APIM_1020.pattern(), clientId);
            return;
        } else if(newStats.getSyncState() == ClientSyncState.UNKNOWN) {
            LOG.info(APIM_1021.pattern(), clientId);
            clientCache.remove(clientId);
            LOG.warn(APIM_2001.pattern(), clientId);
            return;
        }

        // reset usages if the client is suspended or deleted
        if (!cachedStats.isAppWithPermission()) {
            LOG.debug("reset metrics of app without permissions '{}'", clientId);
            resetAllMetrics(cachedStats, clientId);
        }

        // create or update metrics in cache
        createOrUpdateMetrics(newStats, cachedStats);

        // delete metrics from cache
        deleteMetricRestrictionsByPlanChange(newStats, cachedStats);

        // add unlimited metrics if they aren't in the new application plan
        updateToUnlimitedUsageWhenNoUsageIsDefined(cachedStats, serviceId, serviceToMetricsCache::get);

        // delete unlimited metrics if they are removed from the metrics
        deleteUnlimitedMetrics(cachedStats, serviceToMetricsCache, serviceId);

        LOG.debug("Successfully authorized (OAuth) with ClientId='{}' and synchronized usages.", clientId);
    }

    private void resetAllMetrics(Client cachedStats, String clientId) {
        cachedStats.getMetricSysNames()
                .forEach(metricSysName -> resetMetric(metricSysName, clientId, cachedStats));
    }

    private void resetMetric(String metricSysName, String clientId, Client cachedStats) {
        cachedStats.updateMetricUsage(metricSysName, metricUsage -> unlimitedMetric(clientId, metricSysName));
    }

    private void deleteUnlimitedMetrics(Client cachedClient, ServiceToMetricsCache serviceToMetricsCache, String serviceId) {
        List<Metric> metrics = serviceToMetricsCache.get(serviceId);
        List<String> metricsToDelete = new ArrayList<>();

        cachedClient.getMetricSysNames().forEach(metricSysNameInUsage -> {
            if (!metrics.stream()
                    .map(Metric::getSystemName)
                    .anyMatch(s -> s.equals(metricSysNameInUsage))) {
                metricsToDelete.add(metricSysNameInUsage);
            }
        });
        cachedClient.deleteMatchingMetrics(metricsToDelete);

    }

    private void deleteMetricRestrictionsByPlanChange(Client newStats, Client cachedStats) {
        cachedStats.deleteNoneMatchingMetrics(newStats.getMetricSysNames());
    }

    private void createOrUpdateMetrics(Client newStats, Client cachedStats) {
        newStats.getMetricSysNames()
                .forEach(newMetricSysName -> createOrUpdateMetric(newMetricSysName, newStats.getUsage(newMetricSysName), cachedStats));
    }

    private void createOrUpdateMetric(String newMetricSysName, MetricUsage newUsage, Client cachedStats) {
        cachedStats.updateMetricUsage(newMetricSysName, existingUsage -> {
            if (existingUsage == null) {
                LOG.info(APIM_1028.pattern(), newUsage);
                return newUsage; //Add new metric to cache
            }else {
                //We synchronize the existing usage with the from the 3Scale Backend
                return synchronizePeriods(newUsage, existingUsage);
            }
        });
    }

}
