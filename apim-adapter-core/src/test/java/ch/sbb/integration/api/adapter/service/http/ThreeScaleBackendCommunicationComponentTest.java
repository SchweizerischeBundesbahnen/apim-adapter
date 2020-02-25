package ch.sbb.integration.api.adapter.service.http;

import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.model.usage.ClientSyncState;
import ch.sbb.integration.api.adapter.model.usage.MetricUsage;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;

public class ThreeScaleBackendCommunicationComponentTest extends AbstractWiremockTest {

    @Before
    public void initStubs() {
        init();
        WireMock.reset();
    }

    private Client loadClient(String clientId) {
        try {
            return configurationLoader.loadPlanConfig(apimAdapterConfig.getAdapterServiceId(), clientId, false);
        } catch (Exception e) {
            throw new ThreeScaleAdapterException("Error when loading the initial client usage Stats", e);
        }
    }

    /**
     * https://issues.sbb.ch/browse/IAM-467
     * Support for eternity
     */
    @Test
    public void testPlanWithEternity() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithEternity(60, 10);
        Client client = loadClient(CLIENT_ID);
        MetricUsage eternityUsage = client.getUsage("locations");
        MetricUsage periodUsage = client.getUsage("hits");

        Assert.assertEquals(CLIENT_ID, client.getId());
        Assert.assertTrue(client.isAppWithPermission());
        Assert.assertTrue(eternityUsage.isLimited());
        Assert.assertEquals("The Usage with eternity should have the Value of 9999-12-31 23:59:59 +0000", ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, eternityUsage.getPeriodEnd().getZone()), eternityUsage.getPeriodEnd());
        Assert.assertEquals("The Usage with eternity should have the Value of 1970-01-01 00:00:00 +0000", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, eternityUsage.getPeriodStart().getZone()), eternityUsage.getPeriodStart());
        Assert.assertTrue(periodUsage.isLimited());
    }

    /**
     * https://issues.sbb.ch/browse/IAM-469
     * If 3scale returns 409 it should still be possible to hit
     */
    @Test
    public void testPlanWithExceededLimits() throws InterruptedException {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithExceededLimit();
        Client client = loadClient(CLIENT_ID);
        MetricUsage exceededUsage = client.getUsage("locations");
        MetricUsage validUsage = client.getUsage("hits");

        Assert.assertEquals(CLIENT_ID, client.getId());
        Assert.assertTrue("The app should have permission", client.isAppWithPermission());
        Assert.assertTrue(exceededUsage.isLimited());
        System.out.println(client);
        Assert.assertFalse("It should not be possible to hit on this metric", exceededUsage.incrementCurrentUsage());
        Assert.assertEquals("The Usage with eternity should have the Value of 9999-12-31 23:59:59 +0000", ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, exceededUsage.getPeriodEnd().getZone()), exceededUsage.getPeriodEnd());
        Assert.assertEquals("The Usage with eternity should have the Value of 1970-01-01 00:00:00 +0000", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, exceededUsage.getPeriodStart().getZone()), exceededUsage.getPeriodStart());
        Assert.assertTrue("The exceeded Usage should have more reporting than the limit", exceededUsage.getLimit() < exceededUsage.getBase());
        Assert.assertTrue("This usage should be limited", validUsage.isLimited());
        Assert.assertTrue("It should be possible to hit on the other usage", validUsage.incrementCurrentUsage());
    }

    /**
     * https://issues.sbb.ch/browse/IAM-408
     * Handling of clients which aren't from this API (404)
     */
    @Test
    public void testOauthorizeWithInvalidClient() {

        StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();
        Client client = loadClient(CLIENT_ID);

        Assert.assertEquals(CLIENT_ID, client.getId());
        Assert.assertFalse("The app should not have permission", client.isAppWithPermission());
        Assert.assertEquals("3scale should have returned 404 resulting in APPLICATION_NOT_FOUND", ClientSyncState.APPLICATION_NOT_FOUND, client.getSyncState());
    }
}