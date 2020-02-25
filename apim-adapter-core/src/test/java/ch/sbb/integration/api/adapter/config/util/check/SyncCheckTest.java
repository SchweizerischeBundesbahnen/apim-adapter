package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.model.reporting.Hits;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.cache.ClientCache;
import ch.sbb.integration.api.adapter.service.cache.ServiceToMetricsCache;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleScheduler;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleSynchronizerService;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SyncCheckTest extends AbstractWiremockTest {

    private ThreeScaleScheduler scheduler;

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionCheck.class);

    @Before
    public void initStubs() {
        WireMock.reset();
        StubGenerator.instantiateAll();

        ServiceToMetricsCache serviceToMetricsCache = new ServiceToMetricsCache(apimAdapterConfig, configurationLoader);

        ThreeScaleSynchronizerService threeScaleSynchronizer = new ThreeScaleSynchronizerService(
                new ClientCache(serviceToMetricsCache::get, apimAdapterConfig, configurationLoader),
                serviceToMetricsCache,
                apimAdapterConfig.getAdapterServiceId(),
                configurationLoader,
                threeScaleBackendCommunicationComponent,
                emergencyModeState,
                new Hits());
        scheduler = new ThreeScaleScheduler(threeScaleSynchronizer, 1, 1);
        scheduler.scheduleSynchronizationOf3ScaleStats(apimAdapterConfig.getAdapterSyncRateInSeconds());

    }

    @Test
    public void syncCheck() throws InterruptedException {
        int syncRateFactor = 1;
        int syncRateInSeconds = 1;
        SyncCheck syncCheck = SyncCheck.createForTests(syncRateFactor, 1, syncRateInSeconds);
        CheckResult notStartedCheck = syncCheck.syncCheck(scheduler);
        assertFalse(notStartedCheck.isUp());
        assertEquals(Status.DOWN, notStartedCheck.getStatus());

        int counter = 0;
        while (!syncCheck.syncCheck(scheduler).isUp() && counter < 15) {
            LOG.info("Sync has not started yet");
            TimeUnit.MILLISECONDS.sleep(500);
            counter++;
        }

        CheckResult startedCheck = syncCheck.syncCheck(scheduler);
        assertTrue(startedCheck.isUp());
        assertEquals(Status.UP, startedCheck.getStatus());

        scheduler.terminate();

        counter = 0;
        while (syncCheck.syncCheck(scheduler).isUp() && counter < syncRateInSeconds * syncRateFactor * 10) {
            LOG.info("Wait for Sync to stop");
            TimeUnit.MILLISECONDS.sleep(500);
            counter++;
        }

        CheckResult failedCheck = syncCheck.syncCheck(scheduler);

        assertFalse(failedCheck.isUp());

    }
}