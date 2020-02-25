package ch.sbb.integration.api.adapter.service.job;

import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ch.sbb.integration.api.adapter.wiremock.UrlMappingRules.patternForAuthorization;
import static ch.sbb.integration.api.adapter.wiremock.UrlMappingRules.patternForReporting;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

/**
 * Created by u217269 on 12.04.2018.
 */
public class ThreeScaleSynchronizerServiceWiremockTest extends AbstractWiremockTest {

    @Before
    public void initStubs() {
        WireMock.reset();
        StubGenerator.instantiateAll();
    }

    @Test
    public void testReportingTo3Scale_simple() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;

            for (int i = 0; i < 5; i++) {
                AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
                service.reportHit(authRepResponse, 200);
            }

            triggerSync(service);

            verify(postRequestedFor(patternForReporting()));
            List<LoggedRequest> reportingRequests = findAll(postRequestedFor(patternForReporting()));
            StringBuilder decodedBody = new StringBuilder();
            for (LoggedRequest request : reportingRequests) {
                decodedBody.append(URLDecoder.decode(request.getBodyAsString()));
            }
            String decodedBodyString = decodedBody.toString();
            assertFalse(decodedBodyString.contains("[usage][hits]"));
            assertTrue(decodedBodyString.contains("[usage][locations]=5"));
            assertFalse(decodedBodyString.contains("[log][code]")); // 5 hits for metric with name "locations"
        }
    }

    @Test
    public void testReportingTo3Scale_withInvalidClient() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {

            String invalidClientId = CLIENT_ID + "invalid";
            String validToken = TokenGenerator.getInstance().generateBearerToken(invalidClientId, 30_000);
            HttpMethod method = HttpMethod.GET;

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            service.reportHit(authRepResponse, 200);
            triggerSync(service);

            assertTrue("here the client is still valid", authRepResponse.isAllowed());
            verify(postRequestedFor(patternForReporting()));
            List<LoggedRequest> reportingRequests = findAll(postRequestedFor(patternForReporting()));
            StringBuilder decodedBody = new StringBuilder();
            for (LoggedRequest request : reportingRequests) {
                decodedBody.append(URLDecoder.decode(request.getBodyAsString()));
            }
            String decodedBodyString = decodedBody.toString();
            assertTrue(decodedBodyString.contains("[usage][locations]=1"));
            assertTrue("the report should be made wit the correct client", decodedBodyString.contains("[app_id]=" + invalidClientId));

            //Reseting the configuration and initializing Stub with 404 error
            removeAllMappings();
            StubGenerator.instantiateAll();
            StubGenerator.removeStub(StubGenerator.instantiateStubForAuthorizeAndSync());
            StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();

            triggerSync(service);
            service.authRep(validToken, V1_LOCATIONS, method);
            triggerSync(service);
            List<LoggedRequest> beforeReport = findAll(postRequestedFor(patternForReporting()));

            List<AuthRepResponse> responses = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                responses.add(service.authRep(validToken, V1_LOCATIONS, method));
            }
            triggerSync(service);

            assertTrue("no request should be allowed after we 'deleted' the client",
                    responses.stream().noneMatch(AuthRepResponse::isAllowed));

            List<LoggedRequest> afterReport = findAll(postRequestedFor(patternForReporting()));
            assertEquals("the number of request should stay the same", beforeReport.size(), afterReport.size());

            Client stats = service.readCurrentStats(CLIENT_ID);

            assertFalse("Now the stats should be reset to zero", stats.isAppWithPermission());
            assertUsages(stats, "hits", null, 0L);
            assertUsages(stats, "locations", null, 0L);
            assertUsages(stats, "locations-noworries", null, 0L);

        }
    }


    @Test
    public void testReportingTo3Scale_removeOfInvalidClientFromCache() throws InterruptedException {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            // Arrange
            StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();

            String invalidClientId = CLIENT_ID + "invalid";
            String validToken = TokenGenerator.getInstance().generateBearerToken(invalidClientId, 30_000);
            HttpMethod method = HttpMethod.GET;

            // Act 1
            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);

            // Assert 1
            assertFalse("this client should not be valid", authRepResponse.isAllowed());

            List<LoggedRequest> initialLoadOfClient = findAll(getRequestedFor(patternForAuthorization()));
            assertEquals("the first request should request the information from 3scale",
                    1, initialLoadOfClient.size());

            // Act 2
            triggerSync(service);

            // Assert 2
            List<LoggedRequest> loadAgainBecauseSyncRemovedTheClient = findAll(getRequestedFor(patternForAuthorization()));
            loadAgainBecauseSyncRemovedTheClient = findAll(getRequestedFor(patternForAuthorization()));
            assertEquals("We trigger a sync and the client was not chached because it is illegal. So it is synced again",
                    2, loadAgainBecauseSyncRemovedTheClient.size());

            // Act 3
            service.readCurrentStats(CLIENT_ID);

            // Assert 3
            loadAgainBecauseSyncRemovedTheClient = findAll(getRequestedFor(patternForAuthorization()));
            assertEquals("We read the stats from this client again and it was still not cached. Hence a 3rd call to 3scale",
                    3, loadAgainBecauseSyncRemovedTheClient.size());
        }
    }


    @Test
    public void testReportingTo3Scale_verifyUsageStats() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithCustomAttributes(60, 5);
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;

            for (int i = 0; i < 5; i++) {
                service.authRep(validToken, V1_LOCATIONS, method);
            }

            triggerSync(service);

            service.authRep(validToken, V1_LOCATIONS, method);

            Client stats = service.readCurrentStats(CLIENT_ID);
            assertUsages(stats, "hits", 5L, 1L);
            assertUsages(stats, "locations", 5L, 1L);
            assertUsages(stats, "locations-noworries", 0L, 0L);
        }
    }

    /**
     * https://issues.sbb.ch/browse/IAM-467
     */
    @Test
    public void testReportingTo3Scale_withEternityUsage() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithEternity(60, 4);

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;

            for (int i = 0; i < 4; i++) {
                service.authRep(validToken, V1_LOCATIONS, method);
            }

            triggerSync(service);

            service.authRep(validToken, V1_LOCATIONS, method);

            Client stats = service.readCurrentStats(CLIENT_ID);
            assertUsages(stats, "locations", 4L, 1L);
        }
    }


    @Test
    public void testReportingTo3Scale_rollbackOfClientUsageWhenBackendIsNotAvailable() throws ExecutionException, InterruptedException {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            // Arrange
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            stubFor(post(patternForReporting())
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

            HttpMethod method = HttpMethod.GET;

            // Act 1
            for (int i = 0; i < 5; i++) {
                service.authRep(validToken, V1_LOCATIONS, method);
            }

            // Assert 1
            Client stats = service.readCurrentStats(CLIENT_ID);
            assertUsages(stats, "hits", 3L, 5L);
            assertUsages(stats, "locations", 3L, 5L);
            assertUsages(stats, "locations-noworries", 0L, 0L);

            // Act 2
            triggerSync(service);
            service.authRep(validToken, V1_LOCATIONS, method);

            // Assert 2
            stats = service.readCurrentStats(CLIENT_ID);
            assertUsages(stats, "hits", 3L, 1L);
            assertUsages(stats, "locations", 3L, 1L);
            assertUsages(stats, "locations-noworries", 0L, 0L);
        }
    }

    @Test
    public void testReportingTo3Scale_notAcceptUnknownClientsOnServerError() {
        try(ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {

            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID+"500", 30_000);
            HttpMethod method = HttpMethod.GET;

            StubGenerator.instantiateStubForAuthorizeAndSyncWith3ScaleError();
            stubFor(post(patternForReporting())
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);

            assertFalse(authRepResponse.isAllowed());
        }
    }

    @Test
    public void testReportingTo3Scale_serverError() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {

            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);

            assertTrue(authRepResponse.isAllowed());

            StubGenerator.instantiateStubForAuthorizeAndSyncWith3ScaleError();
            stubFor(post(patternForReporting())
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

            triggerSync(service);

            service.authRep(validToken, V1_LOCATIONS, method);

            Client stats = service.readCurrentStats(CLIENT_ID);
            assertTrue(stats.isAppWithPermission());
            assertUsages(stats, "hits", 3L, 2L);
            assertUsages(stats, "locations", 3L, 2L);
        }
    }

    @Test
    public void testReportingTo3Scale_removeClientDuringRuntime() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = HttpMethod.GET;
            StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();

            for (int i = 0; i < 5; i++) {
                service.authRep(validToken, V1_LOCATIONS, method);
            }
            triggerSync(service);

            Client stats = service.readCurrentStats(CLIENT_ID);
            assertFalse(stats.isAppWithPermission());
            assertUsages(stats, "hits", null, 0L);
            assertUsages(stats, "locations", null, 0L);
            assertUsages(stats, "locations-noworries", null, 0L);

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            assertFalse(authRepResponse.isAllowed());
        }
    }


    private void validateResponse(AuthRepResponse badReqeustResponse, AuthRepResponse expectedBadRequestResponse) {
        assertEquals(expectedBadRequestResponse.isAllowed(), badReqeustResponse.isAllowed());
        assertEquals(expectedBadRequestResponse.getHttpStatus(), badReqeustResponse.getHttpStatus());
        assertEquals(expectedBadRequestResponse.getClientId(), badReqeustResponse.getClientId());
    }

    private void triggerSync(ApimAdapterService service) {
		try {
			service.triggerSynchronization().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Test failed due to a synchronization error", e);
		}
	}
}