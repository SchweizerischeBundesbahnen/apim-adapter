package ch.sbb.integration.api.adapter.util;

import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.service.cache.TokenToParsedTokenCache;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by u217269 on 01.03.2018.
 */
public class TokenGeneratorTest extends AbstractWiremockTest {

    @Test
    public void testGenerationOf_validToken() {
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();
        String token = TokenGenerator.getInstance().generateBearerToken("test-client", 30_000);

        TokenToParsedTokenCache cache = new TokenToParsedTokenCache(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());

        OAuthToken oAuthToken = cache.parseToken(token);
        assertFalse("Token should not be expired", oAuthToken.isExpired());
        assertTrue("Token is expected to be valid.", oAuthToken.isValid());
    }

}