package ch.sbb.integration.api.adapter.service.job;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import ch.sbb.integration.api.adapter.util.Utilities;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.List;

import static ch.sbb.integration.api.adapter.wiremock.UrlMappingRules.patternForReporting;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by u217269 on 12.04.2018.
 */
@NotThreadSafe
public class ThreeScaleSchedulerTest extends AbstractWiremockTest {

    private static final Logger LOG = LoggerFactory.getLogger(ApimAdapterConfig.class);

    @Before
    public void initStubs() {
        WireMock.reset();
        WireMock.findAll(postRequestedFor(patternForReporting())).clear();
        StubGenerator.instantiateAll();
    }

    @Test
    public void testGracefulShutdownOfSync() {
        LOG.error("Started testGracefulShutdownOfSync");
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;

            for (int i = 0; i < 50; i++) {
                AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
                service.reportHit(authRepResponse, 200);
            }

            service.close();

            verify(postRequestedFor(patternForReporting()));
        }
        LOG.error("Finished testGracefulShutdownOfSync");
    }

    @Test
    public void testReportingTo3Scale_waitOnSyncScheduler() {
        LOG.error("Started testReportingTo3Scale_waitOnSyncScheduler");
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;
            for (int i = 0; i < 5; i++) {
                AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
                service.reportHit(authRepResponse, 200);
            }

            Utilities.tryToSleep(apimAdapterConfig.getAdapterSyncRateInSeconds() * 1_000 + 1_000);
            verify(postRequestedFor(patternForReporting()));

            List<LoggedRequest> reportingRequests = findAll(postRequestedFor(patternForReporting()));
            LOG.info("Start checks in testReportingTo3Scale_waitOnSyncScheduler");
            StringBuilder decodedBody = new StringBuilder();
            for (LoggedRequest request : reportingRequests) {
                decodedBody.append(URLDecoder.decode(request.getBodyAsString()));
            }
            String decodedBodyString = decodedBody.toString();
            String expectedAppId = "[app_id]=" + CLIENT_ID;
            assertTrue("Report does not contain expected value(" + expectedAppId + "): " + decodedBodyString, decodedBodyString.contains(expectedAppId));
            String expectedLocations = "[usage][locations]=5";
            assertTrue("Report does not contain expected value(" + expectedLocations + "): " + decodedBodyString, decodedBodyString.contains(expectedLocations));
            String unexpectedHits = "[usage][reporting]";
            assertFalse("Hits should not be reported(" + unexpectedHits + "): " + decodedBodyString, decodedBodyString.contains(unexpectedHits));
        }
        LOG.error("Finished testReportingTo3Scale_waitOnSyncScheduler");
    }
}