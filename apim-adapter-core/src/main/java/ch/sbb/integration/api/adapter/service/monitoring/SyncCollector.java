package ch.sbb.integration.api.adapter.service.monitoring;

import ch.sbb.integration.api.adapter.model.ConfigType;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleScheduler;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public class SyncCollector extends Collector {

    private static Logger LOG = LoggerFactory.getLogger(SyncCollector.class);

    private final ThreeScaleScheduler threeScaleScheduler;
    private final EmergencyModeState emergencyModeState;

    public SyncCollector(ThreeScaleScheduler threeScaleScheduler, EmergencyModeState emergencyModeState) {
        this.threeScaleScheduler = threeScaleScheduler;
        this.emergencyModeState = emergencyModeState;
    }

    @Override
    public List<MetricFamilySamples> collect() {

        LOG.debug("collecting sync metrics");
        List<MetricFamilySamples> mfs = new ArrayList<>();
        collectSynchronizationMetrics(mfs);
        collectEmergencyModeMetric(mfs);
        collectRemoteConfigMetrics(mfs);

        return mfs;
    }

    private void collectSynchronizationMetrics(List<MetricFamilySamples> mfs) {
        List<String> labelNames = singletonList("sync");
        List<String> labelValue = singletonList("3scaleSync");

        GaugeMetricFamily syncStart = new GaugeMetricFamily(
                "apim_adapter_sync_last_run_timestamp",
                "The last time the sync started or tried to run",
                labelNames);

        syncStart.addMetric(labelValue, threeScaleScheduler.getSyncStatus().getLastStartTimestamp());
        mfs.add(syncStart);

        GaugeMetricFamily syncSuccess = new GaugeMetricFamily(
                "apim_adapter_sync_successful",
                "the state of the last sync run, 0 = false, 1 = true",
                labelNames);

        syncSuccess.addMetric(labelValue, toDouble(threeScaleScheduler.getSyncStatus().isLastRunSuccessful()));
        mfs.add(syncSuccess);


        GaugeMetricFamily syncDuration = new GaugeMetricFamily(
                "apim_adapter_sync_sync_last_run_duration_seconds",
                "duration of the last sync run",
                labelNames);

        syncDuration.addMetric(labelValue, threeScaleScheduler.getSyncStatus().getLastSyncDurationSeconds());
        mfs.add(syncDuration);
    }

    private void collectEmergencyModeMetric(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily emergencyMode = new GaugeMetricFamily(
                "apim_adapter_emergency_mode",
                "1 means emergency mode is active, 0 means inactive",
                singletonList("state"));

        emergencyMode.addMetric(singletonList("emergency_mode"), toDouble(emergencyModeState.isEmergencyMode()));
        mfs.add(emergencyMode);
    }

    private void collectRemoteConfigMetrics(List<MetricFamilySamples> mfs) {
        GaugeMetricFamily remoteConfigurationSuccessfullyLoaded = new GaugeMetricFamily(
                "apim_adapter_remote_config_successfully_loaded",
                "1 means a remote config could be loaded successfully, 0 means config could not be loaded. If any is 0, emergency_mode is active (1)",
                singletonList("config_type"));
        for (ConfigType configType : ConfigType.values()) {
            remoteConfigurationSuccessfullyLoaded.addMetric(singletonList(configType.getId()), toDouble(emergencyModeState.isConfigurationSuccessfulLoaded(configType)));
        }
        mfs.add(remoteConfigurationSuccessfullyLoaded);
    }

    private double toDouble(boolean bool) {
        return bool ? 1 : 0;
    }
}
