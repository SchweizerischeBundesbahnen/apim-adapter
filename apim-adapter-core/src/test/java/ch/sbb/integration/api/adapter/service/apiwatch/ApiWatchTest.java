package ch.sbb.integration.api.adapter.service.apiwatch;

import ch.sbb.integration.api.adapter.service.cache.TokenToParsedTokenCache;
import ch.sbb.integration.api.adapter.service.configuration.OperationMode;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiWatchTest extends AbstractWiremockTest {

    @Test
    public void isApiWatchRequest() {
        // arrange
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();

        final TokenToParsedTokenCache tokenToParsedTokenCache = new TokenToParsedTokenCache(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());
        final ApiWatch apiWatch = new ApiWatch(apimAdapterConfig, OfflineConfigurationCacheRepo.disabled(), tokenToParsedTokenCache, OperationMode.ADAPTER_JAVA);

        final String apiWatchClientToken = TokenGenerator.getInstance().generateBearerToken("api-watch-client", 30_000);
        final String someOtherClientToken = TokenGenerator.getInstance().generateBearerToken("some-other-client", 30_000);

        // act / assert
        assertTrue(apiWatch.isApiWatchRequest(apiWatchClientToken, "GET"));
        assertTrue(apiWatch.isApiWatchRequest(apiWatchClientToken, HttpMethod.GET));
        assertFalse(apiWatch.isApiWatchRequest(apiWatchClientToken, HttpMethod.PUT));

        assertTrue(apiWatch.isApiWatchRequest(tokenToParsedTokenCache.parseToken(apiWatchClientToken), "GET"));
        assertFalse(apiWatch.isApiWatchRequest(tokenToParsedTokenCache.parseToken(apiWatchClientToken), "PUT"));

        assertFalse(apiWatch.isApiWatchRequest(tokenToParsedTokenCache.parseToken(someOtherClientToken), "GET"));
        assertFalse(apiWatch.isApiWatchRequest(tokenToParsedTokenCache.parseToken(someOtherClientToken), "PUT"));
    }


    @Test
    public void buildResponse() throws JsonProcessingException {
        // arrange
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();

        final TokenToParsedTokenCache tokenToParsedTokenCache = new TokenToParsedTokenCache(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());
        final ApiWatch apiWatch = new ApiWatch(apimAdapterConfig, OfflineConfigurationCacheRepo.disabled(), tokenToParsedTokenCache, OperationMode.ADAPTER_JAVA);

        // act
        final String resp = apiWatch.buildResponse("-");

        // assert
        assertTrue(resp.contains("\"operationMode\":\"ADAPTER_JAVA\""));
        assertTrue(resp.contains("\"offlineConfigurationCache\":\"DISABLED\""));
        assertTrue(resp.contains("\"backendResponse\":\"-\""));
    }

    @Test
    public void pingBackend() {
        // arrange
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();
        stubFor(get(urlMatching("/backendEndpoint"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withBody("backend response here")
                        .withStatus(SC_OK)));


        final TokenToParsedTokenCache tokenToParsedTokenCache = new TokenToParsedTokenCache(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());
        final ApiWatch apiWatch = new ApiWatch(apimAdapterConfig, OfflineConfigurationCacheRepo.disabled(), tokenToParsedTokenCache, OperationMode.ADAPTER_JAVA);

        // act
        final String apiWatchClientToken = TokenGenerator.getInstance().generateBearerToken("api-watch-client", 30_000);
        String backendPingResponse = apiWatch.pingBackend(apiWatchClientToken, "http://localhost:8099/backendEndpoint");

        // assert
        assertEquals("backend response here", backendPingResponse);
    }
}