package ch.sbb.integration.api.threescale.service.job;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by u217269 on 20.02.2018.
 */
public class ThreeScaleScheduler {

    private final ThreeScaleSynchronizerService threeScaleSynchronizerService;

    public ThreeScaleScheduler(ThreeScaleSynchronizerService threeScaleSynchronizerService) {
        this.threeScaleSynchronizerService = threeScaleSynchronizerService;
    }

    public void scheduleSynchronizationOf3ScaleStats(int syncRateInSeconds) {

        Timer timer = new Timer("SynchronizationOf3ScaleStats-Timer");
        long period = 1_000L * syncRateInSeconds;
        long delay = period;
        timer.scheduleAtFixedRate(threeScaleSynchronizerService, delay, period);

    }

    public Future triggerSynchronization() {
        return Executors.newSingleThreadExecutor().submit(threeScaleSynchronizerService);
    }

}
