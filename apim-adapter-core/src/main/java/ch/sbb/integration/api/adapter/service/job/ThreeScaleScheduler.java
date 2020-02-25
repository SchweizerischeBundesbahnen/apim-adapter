package ch.sbb.integration.api.adapter.service.job;

import ch.sbb.integration.api.adapter.model.status.SyncStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by u217269 on 20.02.2018.
 */
public class ThreeScaleScheduler {

    public static final int INITIAL_DELAY = 5;
    public static final int TERMINATION_TIMEOUT = 20;
    private final ThreeScaleSynchronizerService threeScaleSynchronizerService;
    private final ScheduledExecutorService scheduler;

    private final int initialDelay;
    private final int terminationTimeout;

    public ThreeScaleScheduler(ThreeScaleSynchronizerService threeScaleSynchronizerService, int initialDelay, int terminationTimeout) {
        this.threeScaleSynchronizerService = threeScaleSynchronizerService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.initialDelay = initialDelay;
        this.terminationTimeout = terminationTimeout;
    }

    public ThreeScaleScheduler(ThreeScaleSynchronizerService threeScaleSynchronizerService) {
        this(threeScaleSynchronizerService, INITIAL_DELAY, TERMINATION_TIMEOUT);
    }

    public void scheduleSynchronizationOf3ScaleStats(int syncRateInSeconds) {
        scheduler.scheduleWithFixedDelay(threeScaleSynchronizerService, initialDelay, syncRateInSeconds, TimeUnit.SECONDS);
    }

    public Future triggerSynchronization() {
        return scheduler.submit(threeScaleSynchronizerService);
    }

    public SyncStatus getSyncStatus() {
        return threeScaleSynchronizerService.getSyncStatus();
    }

    public void terminate() throws InterruptedException {
        scheduler.shutdown();
        scheduler.awaitTermination(terminationTimeout, TimeUnit.SECONDS);
    }

    public boolean isTerminated() {
        return scheduler.isTerminated();
    }
}
