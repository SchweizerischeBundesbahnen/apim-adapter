package ch.sbb.integration.api.adapter.model.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

public class SyncStatus {
    private static final Logger LOG = LoggerFactory.getLogger(SyncStatus.class);

    private boolean lastRunSuccessful = false;
    private ZonedDateTime lastSyncStart;
    private Duration lastSyncDuration = Duration.ZERO;

    /**
     * Stores the last synchronization activity point in time - this is updated more frequently than lastSyncStart
     */
    private ZonedDateTime lastSyncActivity;

    public boolean isLastRunSuccessful() {
        return lastRunSuccessful;
    }

    public void setLastRunSuccessful(boolean lastRunSuccessful) {
        this.lastRunSuccessful = lastRunSuccessful;
    }

    public double getLastSyncDurationSeconds() {
        return lastSyncDuration.getSeconds() + (double)lastSyncDuration.getNano() / 1_000_000_000.0;
    }

    public void startSync() {
        lastSyncStart = now();
        LOG.debug("BatchSynchronizeStatsJob triggered at '{}'", lastSyncStart);
        updateLastSyncActivity();
    }

    public void finishSync() {
        final ZonedDateTime lastSyncFinished = now();
        updateLastSyncActivity();

        if(lastSyncFinished.isAfter(lastSyncStart)) {
            lastSyncDuration = Duration.between(lastSyncStart, lastSyncFinished);
        } else {
            lastSyncDuration = Duration.ZERO;
        }
        LOG.debug("finished BatchSynchronizeStatsJob at '{}'", lastSyncFinished);
        LOG.debug("Batch-Job duration: '{}' seconds.", getLastSyncDurationSeconds());
    }

    public long getLastStartTimestamp() {
        if (lastSyncStart == null) {
            return -1;
        }
        return lastSyncStart.toInstant().toEpochMilli();
    }

    public ZonedDateTime getLastSyncActivity() {
        return lastSyncActivity;
    }

    public void updateLastSyncActivity() {
        this.lastSyncActivity = now();
        LOG.debug("lastSyncActivity at '{}'", lastSyncActivity);
    }
}
