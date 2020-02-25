package ch.sbb.integration.api.adapter.service;

import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.model.OAuthToken.TokenStateEnum;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuer;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuerStore;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.service.utils.AuthUtils;
import ch.sbb.integration.api.adapter.service.utils.ErrorReason;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static ch.sbb.integration.api.adapter.service.utils.HttpMethod.GET;
import static ch.sbb.integration.api.adapter.service.utils.HttpMethod.POST;
import static ch.sbb.integration.api.adapter.service.utils.HttpMethod.PUT;
import static ch.sbb.integration.api.adapter.wiremock.UrlMappingRules.patternForAuthorization;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

/**
 * Created by u217269 on 23.02.2018.
 */
public class ApimAdapterServiceTest extends AbstractWiremockTest {

    @Before
    public void initStubs() {
        WireMock.reset();
        StubGenerator.instantiateAll();
    }

    @Test
    public void testStandardRequestWithUnlimitedPlan() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            stubFor(get(patternForAuthorization())
                    .atPriority(5)
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.SC_OK)
                            .withBody(StubGenerator.authResponseForAnUnlimitedPlan())));

            HttpMethod method = GET;
            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);

            assertTrue(authRepResponse.isAllowed());
            assertEquals(CLIENT_ID, authRepResponse.getClientId());
            assertNotNull(authRepResponse.getMessage());
        }
    }

    @Test
    public void testStandardRequestWithinLimits() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);

            HttpMethod method = GET;
            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);

            assertTrue(authRepResponse.isAllowed());
            assertEquals(authRepResponse.getClientId(), CLIENT_ID);
            assertNotNull(authRepResponse.getMessage());
        }
    }

    @Test
    public void testStandardRequestExceedingLimits() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            String path = V1_LOCATIONS;
            HttpMethod method = GET;
            AuthRepResponse expectedAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.LIMIT_EXCEEDED, path, null, method, emptyList());


            AuthRepResponse authRepResponse = service.authRep(validToken, path, method);
            assertTrue(authRepResponse.isAllowed());
            String okMessage = authRepResponse.getMessage();

            for (int i = 0; i < 5; i++) {
                authRepResponse = service.authRep(validToken, path, method);
            }
            assertFalse(authRepResponse.isAllowed());
            assertNotNull(authRepResponse.getMessage());
            assertEquals(expectedAuthRepResponse.getHttpStatus(), authRepResponse.getHttpStatus());
            assertEquals(expectedAuthRepResponse.getMessage(), authRepResponse.getMessage());
            assertEquals(expectedAuthRepResponse.getClientId(), authRepResponse.getClientId());
            assertNotEquals(okMessage, authRepResponse.getMessage());
        }
    }

    @Test
    public void testRequestWithoutPermission() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String invalidToken = TokenGenerator.getInstance().generateBearerToken("someInvalidClientId", 30_000);
            HttpMethod method = GET;

            AuthRepResponse authRepResponse = service.authRep(invalidToken, V1_LOCATIONS, method);

            AuthRepResponse expectedAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, "someInvalidClientId", ErrorReason.CLIENT_ID_HAS_NO_PERMISSION, V1_LOCATIONS, null, method, emptyList());
            validateDisallowedResponse(authRepResponse, expectedAuthRepResponse);
        }
    }

    /**
     * https://issues.sbb.ch/browse/IAM-365
     */
    @Test
    public void testAccessWithClientIdOnly() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            HttpMethod method = GET;
            AuthRepResponse authRepResponse = service.authRepWithClientId(AuthUtils.RH_SSO_REALM, CLIENT_ID, V1_LOCATIONS, "", method);

            assertTrue(authRepResponse.isAllowed());
            assertEquals(authRepResponse.getClientId(), CLIENT_ID);
            assertNotNull(authRepResponse.getMessage());
        }
    }

    /**
     * https://issues.sbb.ch/browse/IAM-365
     */
    @Test
    public void testAccessWithClientIdOnly_withInvalidClientId() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            HttpMethod method = GET;

            AuthRepResponse authRepResponse = service.authRepWithClientId(AuthUtils.RH_SSO_REALM, "someInvalidClientId", V1_LOCATIONS, "", method);

            AuthRepResponse expectedAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, "someInvalidClientId", ErrorReason.CLIENT_ID_HAS_NO_PERMISSION, V1_LOCATIONS, null, method, emptyList());
            validateDisallowedResponse(authRepResponse, expectedAuthRepResponse);
        }
    }

    @Test
    public void testRequestWithInvalidToken() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String invalidToken = TokenGenerator.getInstance().generateBearerToken(null, 30_000);
            String expiredToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 0);
            HttpMethod method = GET;

            AuthRepResponse expectedInvalidTokenAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, null, ErrorReason.EXPIRED_OR_INVALID, V1_LOCATIONS, null, method, emptyList());
            AuthRepResponse expectedExpiredTokenAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.EXPIRED_OR_INVALID, V1_LOCATIONS, null, method, emptyList());

            AuthRepResponse authRepResponse = service.authRep(invalidToken, V1_LOCATIONS, method);
            AuthRepResponse secondAuthResponse = service.authRep(expiredToken, V1_LOCATIONS, method);

            validateDisallowedResponse(authRepResponse, expectedInvalidTokenAuthRepResponse);

            validateDisallowedResponse(secondAuthResponse, expectedExpiredTokenAuthRepResponse);
        }
    }

    @Test
    public void testRequestWithNewToken() {

        StubGenerator.instantiateStubForAuthorizeAndSync();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String newToken = TokenGenerator.getInstance().generateTokenWithNewKey(CLIENT_ID, 30_000);
            String oldToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 25_000);
            String oldUnknownToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 15_000);
            String invalideToken = TokenGenerator.getInstance().generateTokenWithRandomNewKey(CLIENT_ID, 27_000, true);
            String invalideTokenWithoutKeyId = TokenGenerator.getInstance().generateTokenWithRandomNewKey(CLIENT_ID, 22_000, true);
            HttpMethod method = GET;

            AuthRepResponse expectedInvalidTokenAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.EXPIRED_OR_INVALID, V1_NEW_LOCATIONS, null, method, emptyList());

            AuthRepResponse oldPublicKeyResponse = service.authRep(oldToken, V1_LOCATIONS, method);

            StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithNewKey();

            AuthRepResponse authRepResponse = service.authRep(newToken, V1_NEW_LOCATIONS, method);
            AuthRepResponse secondAuthResponse = service.authRep(oldToken, V1_NEW_LOCATIONS, method);
            AuthRepResponse oldUnknownTokenAuthResponse = service.authRep(oldUnknownToken, V1_NEW_LOCATIONS, method);
            AuthRepResponse invalideTokenAuthResponse = service.authRep(invalideToken, V1_NEW_LOCATIONS, method);
            AuthRepResponse invalideTokenWithoutKeyIdAuthResponse = service.authRep(invalideToken, V1_LOCATIONS, method);

            assertTrue("old token should work with old Public Key", oldPublicKeyResponse.isAllowed());
            assertTrue("new Token should be allowed", authRepResponse.isAllowed());
            assertTrue("old token should be allowed", secondAuthResponse.isAllowed());
            assertTrue("old unknown token should be allowed", oldUnknownTokenAuthResponse.isAllowed());
            assertFalse("old invalid token should not be allowed", invalideTokenWithoutKeyIdAuthResponse.isAllowed());

            validateDisallowedResponse(invalideTokenAuthResponse, expectedInvalidTokenAuthRepResponse);


        }
    }

    @Test
    public void testRequestWithNewTokenButWithSsoError() throws InterruptedException {

        StubGenerator.instantiateStubForAuthorizeAndSync();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String newToken = TokenGenerator.getInstance().generateTokenWithNewKey(CLIENT_ID, 30_000);
            String oldToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 25_000);
            HttpMethod method = GET;

            AuthRepResponse oldPublicKeyResponse = service.authRep(oldToken, V1_LOCATIONS, method);

            StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithError();
            AuthRepResponse newTokenWithSsoUnavailable = service.authRep(newToken, V1_NEW_LOCATIONS, method);
            AuthRepResponse secondAuthResponse = service.authRep(oldToken, V1_NEW_LOCATIONS, method);

            StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithNewKey();
            // make sure reloading occurs
            TokenIssuerStore.getInstance().getIssuers().forEach(TokenIssuer::resetJwksReloadTimestamp);
            AuthRepResponse newTokenWithSsoAvailable = service.authRep(newToken, V1_NEW_LOCATIONS, method);
            AuthRepResponse oldPublicKeyResponseTwo = service.authRep(oldToken, V1_LOCATIONS, method);

            assertTrue("old token should work with old Public Key", oldPublicKeyResponse.isAllowed());
            assertFalse("new Token should be denied if no PK could be loaded", newTokenWithSsoUnavailable.isAllowed());
            assertTrue("Internal Server error or 401 should be returned",
                    HttpStatus.SC_INTERNAL_SERVER_ERROR == newTokenWithSsoUnavailable.getHttpStatus() ||
                            HttpStatus.SC_UNAUTHORIZED == newTokenWithSsoUnavailable.getHttpStatus());
            assertTrue("old token should be allowed, even if the sso is not available", secondAuthResponse.isAllowed());
            assertTrue("new token should be allowed if the SSO is available to load the PK", newTokenWithSsoAvailable.isAllowed());
            assertTrue("old token should be allowed, even if the SSO is back online", oldPublicKeyResponseTwo.isAllowed());


        }
    }


    @Test
    public void testRequestWithoutKeyId() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithNewPlan();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String invalidToken = TokenGenerator.getInstance().generateTokenWithRandomNewKey(CLIENT_ID, 30_000, false);
            HttpMethod method = GET;

            AuthRepResponse expectedInvalidTokenAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.EXPIRED_OR_INVALID, V1_LOCATIONS, null, method, emptyList());

            AuthRepResponse authRepResponse = service.authRep(invalidToken, V1_LOCATIONS, method);

            validateDisallowedResponse(authRepResponse, expectedInvalidTokenAuthRepResponse);

        }
    }

    @Test
    public void testRequestWithWrongSignedToken() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithNewPlan();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String invalidToken = TokenGenerator.getInstance().generateTokenWithRandomNewKey(CLIENT_ID, 30_000, true);
            HttpMethod method = GET;

            AuthRepResponse expectedInvalidTokenAuthRepResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.EXPIRED_OR_INVALID, V1_LOCATIONS, null, method, emptyList());

            AuthRepResponse authRepResponse = service.authRep(invalidToken, V1_LOCATIONS, method);

            validateDisallowedResponse(authRepResponse, expectedInvalidTokenAuthRepResponse);

        }
    }

    @Test
    public void testRequestWithWrongMethod() {
        StubGenerator.instantiateStubForAuthorizeAndSync();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = PUT;

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);

            AuthRepResponse expectedMethodNotFoundResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.METHOD_NOT_FOUND, V1_LOCATIONS, null, method, emptyList());

            validateDisallowedResponse(authRepResponse, expectedMethodNotFoundResponse);

        }
    }

    /**
     * https://issues.sbb.ch/browse/IAM-469
     */
    @Test
    public void testWithExceededMetrics() {
        StubGenerator.instantiateStubForAuthorizeAndSyncWithExceededLimit();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {

            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = GET;

            List<AuthRepResponse> respones = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                respones.add(service.authRep(validToken, V1_LOCATIONS, method));
            }

            AuthRepResponse success = service.authRep(validToken, V1_NEW_LOCATIONS, method);

            assertFalse("No request should be allowed", respones.stream().anyMatch(AuthRepResponse::isAllowed));
            assertTrue("Other request should be allowed", success.isAllowed());

        }
    }


    @Test
    public void testBadRequest() {
        StubGenerator.instantiateStubForAuthorizeAndSync();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);

            OAuthToken oAuthToken = new OAuthToken(TokenStateEnum.VALID, null, validToken, CLIENT_ID, null, null);
            HttpMethod method = PUT;

            AuthRepResponse badPathResponse = service.authRep(validToken, null, method);
            AuthRepResponse badMethodResponse = service.authRep(validToken, V1_LOCATIONS, null);
            AuthRepResponse badReqeustResponse = service.authRep(validToken, null, null);
            AuthRepResponse noTokenResponse = service.authRep(null, V1_LOCATIONS, GET);

            AuthRepResponse expectedBadPathResponse = errorResponseHelper.createErrorAuthResponse(oAuthToken, ErrorReason.BAD_REQUEST, null, null, method, emptyList());
            AuthRepResponse expectedBadMethodResponse = errorResponseHelper.createErrorAuthResponse(oAuthToken, ErrorReason.BAD_REQUEST, V1_LOCATIONS, null, null, emptyList());
            AuthRepResponse expectedBadRequestResponse = errorResponseHelper.createErrorAuthResponse(oAuthToken, ErrorReason.BAD_REQUEST, null, null, null, emptyList());
            AuthRepResponse expectedNoTokenResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, null, ErrorReason.UNAUTHORIZED, V1_LOCATIONS, null, GET, emptyList());

            validateDisallowedResponse(badPathResponse, expectedBadPathResponse);

            validateDisallowedResponse(badMethodResponse, expectedBadMethodResponse);

            validateDisallowedResponse(badReqeustResponse, expectedBadRequestResponse);

            validateDisallowedResponse(noTokenResponse, expectedNoTokenResponse);
        }
    }

    private void validateDisallowedResponse(AuthRepResponse badRequestResponse, AuthRepResponse expectedBadRequestResponse) {
        assertFalse("Access must be denied", badRequestResponse.isAllowed());
        assertEquals(expectedBadRequestResponse.getHttpStatus(), badRequestResponse.getHttpStatus());
        assertEquals(expectedBadRequestResponse.getMessage(), badRequestResponse.getMessage());
        assertEquals(expectedBadRequestResponse.getClientId(), badRequestResponse.getClientId()); // actual=null, expected=""
    }

    @Test
    public void testRequestWithWrongPath() {
        StubGenerator.instantiateStubForAuthorizeAndSync();
        StubGenerator.threescaleAdminApplicationStub();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            String path = "/iHopeNobodyUsesThisPath";
            HttpMethod method = GET;

            AuthRepResponse authRepResponse = service.authRep(validToken, path, method);

            AuthRepResponse expectedMethodNotFoundResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.PATH_NOT_FOUND, path, null, method, emptyList());

            validateDisallowedResponse(authRepResponse, expectedMethodNotFoundResponse);

        }
    }


    @Test
    public void testRequestWithPathMethodAndQueryParamCombination() {
        //Arrange
        StubGenerator.instantiateStubForAuthorizeAndSync();
        StubGenerator.threescaleAdminApplicationStub();

        //For hits there exists the following mapping rules:
        //GET /v2/swisspass/.+/SwissPassService?foo=.+&bar=.+.*
        //POST /v1/.*
        //==> GET on /v2/swisspass/.+/SwissPassService?foo=.+&bar=.+.* should result in 200

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            String path = "/v2/swisspass/1/SwissPassService";
            String queryString = "foo=foo&bar=bar";
            HttpMethod method = GET;

            //Act
            AuthRepResponse authRepResponse = service.authRep(validToken, path, queryString, method);

            //Assert
            assertTrue(authRepResponse.isAllowed());
            assertEquals(CLIENT_ID, authRepResponse.getClientId());
            assertNotNull(authRepResponse.getMessage());
        }
    }


    @Test
    public void testRequestWithWrongQueryString() {
        //Arrange
        StubGenerator.instantiateStubForAuthorizeAndSync();
        StubGenerator.threescaleAdminApplicationStub();

        //For hits there exists the following mapping rules:
        //GET /v2/swisspass/.+/SwissPassService?foo=.+&bar=.+.*
        //POST /v1/.*
        //==> GET on /v2/swisspass/.+/SwissPassService?baz=.+&bar=.+.* should result in 404

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            String path = "/v2/swisspass/1/SwissPassService";
            String queryString = "baz=baz&bar=bar";
            HttpMethod method = GET;

            //Act
            AuthRepResponse authRepResponse = service.authRep(validToken, path, queryString, method);

            //Assert
            AuthRepResponse expectedMethodNotFoundResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.PATH_NOT_FOUND, path, queryString, method, emptyList());
            validateDisallowedResponse(authRepResponse, expectedMethodNotFoundResponse);
        }
    }


    @Test
    public void testRequestWithWrongPathMethodCombination() {
        //Arrange
        StubGenerator.instantiateStubForAuthorizeAndSync();
        StubGenerator.threescaleAdminApplicationStub();

        //For hits there exists the following mapping rules:
        //GET /v2/swisspass/.+/SwissPassService?foo=.+&bar=.+.*
        //POST /v1/.*
        //==> POST on /v2/swisspass/.+/SwissPassService?foo=.+&bar=.+.* should result in 405

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            String path = "/v2/swisspass/1/SwissPassService";
            String queryString = "foo=foo&bar=bar";
            HttpMethod method = POST;

            //Act
            AuthRepResponse authRepResponse = service.authRep(validToken, path, queryString, method);

            //Assert
            AuthRepResponse expectedMethodNotFoundResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.METHOD_NOT_FOUND, path, queryString, method, emptyList());
            validateDisallowedResponse(authRepResponse, expectedMethodNotFoundResponse);
        }
    }

    /**
     * Test for https://issues.sbb.ch/browse/STTIN-900
     */
    @Test
    public void testChangeSubscriptionToAnotherPlan() throws InterruptedException {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = GET;

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            assertTrue(authRepResponse.isAllowed());

            for (int i = 0; i < 5; i++) {
                authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            }
            assertFalse(authRepResponse.isAllowed());

            StubMapping stubOverride = StubGenerator.instantiateStubForAuthorizeAndSyncWithNewPlan();
            service.triggerSynchronization();
            //TODO: Lukas fragen wie das geht... ohne das wird der Sync erst nach abschluss des tests gemacht
            // mit future versucht... bin unfähig
            Thread.sleep(1_000);

            authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            StubGenerator.removeStub(stubOverride);

            assertTrue("Access after Plan-Update should be allowed", authRepResponse.isAllowed());

            Client client = service.readCurrentStats(CLIENT_ID);
            assertTrue("hits Metric should be still limited after update.", client.getUsage("hits").isLimited());
            assertFalse("locations Metric should be unlimited after update.", client.getUsage("locations").isLimited());
            assertFalse("locations-noworries Metric should be unlimited after update.", client.getUsage("locations-noworries").isLimited());

            assertEquals("Base does not match.", 5L, client.getUsage("hits").getBase().longValue());
            assertEquals("Count does not match.", 1L, client.getUsage("hits").getCurrentUsage().get());
        }
    }

    /**
     * Test for https://issues.sbb.ch/browse/STTIN-900
     */
    @Test
    public void testModifyAnApplicationPlan() {
        // Todo: implement testModifyAnApplicationPlan()
        // TODO write this test!
    }

    /**
     * Test for https://issues.sbb.ch/browse/STTIN-900
     */
    @Test
    public void testSuspendAnApplication() throws InterruptedException {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = GET;

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            assertTrue("Request of a 'live' application must be allowed.", authRepResponse.isAllowed());

            StubMapping stubMapping = StubGenerator.instantiateStubForAuthorizeAndSyncAfterApplicationSuspention();
            service.triggerSynchronization();
            //TODO: Lukas fragen wie das geht... ohne das wird der Sync erst nach abschluss des tests gemacht
            // mit future versucht... bin unfähig
            Thread.sleep(1_000);
            authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            StubGenerator.removeStub(stubMapping);

            assertFalse("Request must be denied after suspending the subscription.", authRepResponse.isAllowed());
        }
    }

    /**
     * Test for https://issues.sbb.ch/browse/STTIN-900
     */
    @Test
    public void testDeleteAnApplication() throws InterruptedException {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken("someClientOfANonexistingApplication", 30_000);
            HttpMethod method = GET;

            AuthRepResponse authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            assertTrue("Request of a 'live' application must be allowed.", authRepResponse.isAllowed());

            StubMapping stubMapping = StubGenerator.instantiateStubForAuthorizeAndSyncWithInvalidClientId();
            service.triggerSynchronization();
            //TODO: Lukas fragen wie das geht... ohne das wird der Sync erst nach abschluss des tests gemacht
            // mit future versucht... bin unfähig
            Thread.sleep(1_000);
            authRepResponse = service.authRep(validToken, V1_LOCATIONS, method);
            StubGenerator.removeStub(stubMapping);

            assertFalse("Request must be denied after deleting the application.", authRepResponse.isAllowed());
        }
    }

    /**
     * Test for https://issues.sbb.ch/browse/IAM-470
     */
    @Test
    public void testReportingTo3Scale_verifyCorrectPatternMatch() {
        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = GET;


            service.authRep(validToken, V1_REGEXTEST1, method);
            Client stats = service.readCurrentStats(CLIENT_ID);

            assertUsages(stats, "hits", 3L, 1L);
            assertUsages(stats, "single-wildcard-pattern", 3L, 1L);        // ==> hit
            assertUsages(stats, "double-wildcard-pattern", 3L, 0L);        // ==> not hit
            assertUsages(stats, "single-wildcard-dollar-pattern", 3L, 1L);    // ==> hit

            service.authRep(validToken, V2_REGEXTEST4, "foo=foo&bar=bar", method);
            stats = service.readCurrentStats(CLIENT_ID);

            assertUsages(stats, "hits", 3L, 1L);
            assertUsages(stats, "single-wildcard-pattern", 3L, 1L);        // ==> hit
            assertUsages(stats, "double-wildcard-pattern", 3L, 0L);        // ==> not hit
            assertUsages(stats, "single-wildcard-dollar-pattern", 3L, 1L);    // ==> hit

            service.authRep(validToken, V1_REGEXTEST2, method);
            stats = service.readCurrentStats(CLIENT_ID);

            assertUsages(stats, "single-wildcard-pattern", 3L, 1L);        // ==> no hit (unchanged)
            assertUsages(stats, "double-wildcard-pattern", 3L, 1L);        // ==> hit
            assertUsages(stats, "single-wildcard-dollar-pattern", 3L, 1L);    // ==> no hit

            service.authRep(validToken, V1_REGEXTEST3, method);
            stats = service.readCurrentStats(CLIENT_ID);

            assertUsages(stats, "single-wildcard-pattern", 3L, 2L);        // ==> again hit
            assertUsages(stats, "double-wildcard-pattern", 3L, 1L);        // ==> no hit
            assertUsages(stats, "single-wildcard-dollar-pattern", 3L, 1L); // ==> not hit because of $

        }
    }

    /**
     * Test for https://issues.sbb.ch/browse/AITG-92
     */
    @Test
    public void testReportingTo3Scale_verifyWrongQueryParam() {
        StubGenerator.instantiateStubForAuthorizeAndSync();
        StubGenerator.threescaleAdminApplicationStub();

        try (ApimAdapterService service = ApimAdapterFactory.createApimAdapterService()) {
            String validToken = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
            HttpMethod method = GET;

            final String invalidQueryString = "foo=foo&wrong-bar=bar";

            AuthRepResponse authRepResponse = service.authRep(validToken, V2_REGEXTEST4, invalidQueryString, method);

            AuthRepResponse expectedMethodNotFoundResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, CLIENT_ID, ErrorReason.PATH_NOT_FOUND, V2_REGEXTEST4, invalidQueryString, method, emptyList());
            validateDisallowedResponse(authRepResponse, expectedMethodNotFoundResponse);
        }
    }


}
