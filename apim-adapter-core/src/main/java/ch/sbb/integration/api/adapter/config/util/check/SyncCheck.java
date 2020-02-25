package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.ZonedDateTime.now;

public class SyncCheck {
    private static final Logger LOG = LoggerFactory.getLogger(SyncCheck.class);

    private static final String NAME = "SyncRunningCheck";
    private static final int SYNC_RATE_FACTOR = 5;
    private static final int MIN_THRESHOLD_SECONDS = 5 * 60; // 5 minutes

    private final int syncRateInSeconds;
    /**
     * This threshold is used in order to check if the synchronization is still running.
     * If last sync activity ran during these number of seconds, synchronization is considered running
     */
    private final int lastSyncThresholdSeconds;

    private SyncCheck(final int syncRateFactor, final int minThresholdSeconds, final int syncRateInSeconds) {
        this.syncRateInSeconds = syncRateInSeconds;

        this.lastSyncThresholdSeconds = Math.max(minThresholdSeconds, syncRateFactor * syncRateInSeconds);
        LOG.info("lastSyncThresholdSeconds={}", lastSyncThresholdSeconds);
    }

    public SyncCheck(ApimAdapterConfig config) {
        this(SYNC_RATE_FACTOR, MIN_THRESHOLD_SECONDS, config.getAdapterSyncRateInSeconds());
    }

    public CheckResult syncCheck(ThreeScaleScheduler scheduler) {
        if (scheduler.getSyncStatus().getLastSyncActivity() == null) {
            return new CheckResult(NAME, Status.DOWN, "The 3Scale sync has not started yet");
        } else {
            if (now().minusSeconds(lastSyncThresholdSeconds).isAfter(scheduler.getSyncStatus().getLastSyncActivity())) {
                String msg = ReasonCode.APIM_2004.format(lastSyncThresholdSeconds, syncRateInSeconds);
                LOG.warn(msg);
                return new CheckResult(NAME, Status.DOWN, msg);
            } else {
                return new CheckResult(NAME, Status.UP, "The 3Scale sync is running");
            }
        }
    }

    static SyncCheck createForTests(final int syncRateFactor, final int minThresholdSeconds, final int syncRateInSeconds) {
        return new SyncCheck(syncRateFactor, minThresholdSeconds, syncRateInSeconds);
    }
}
