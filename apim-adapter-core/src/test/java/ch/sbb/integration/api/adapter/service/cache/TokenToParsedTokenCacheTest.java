package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by u217269 on 28.02.2018.
 */
public class TokenToParsedTokenCacheTest extends AbstractWiremockTest {

    protected final String clientId = "084e8c30";
    private TokenToParsedTokenCache cache;

    @Before
    public void before() {
        WireMock.reset();
        StubGenerator.instantiateAll();
        cache = new TokenToParsedTokenCache(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());
    }

    @Test
    public void testTokenParsing_validToken() {


        OAuthToken oAuthToken = cache.parseToken(TokenGenerator.getInstance().generateToken(clientId, 30L, "Bearer"));

        assertNotNull("Result from TokenToParsedTokenCache.parseToken(...) should never be null.", oAuthToken);
        assertEquals("ClientId does not match.", clientId, oAuthToken.getClientId());
        assertTrue("Token is expected to be valid.", oAuthToken.isValid());
        assertFalse("Token is expected not to be expired.", oAuthToken.isExpired());
        assertEquals("Token type should be BEARER.", "Bearer", oAuthToken.getType());
    }

    @Test
    public void testTokenParsing_withCorruptToken() {
        OAuthToken oAuthToken = cache.parseToken("some-invalid-string-token");

        assertNotNull("Result from TokenToParsedTokenCache.parseToken(...) should never be null.", oAuthToken);
        assertNull("ClientId does not match.", oAuthToken.getClientId());
        assertFalse("Token is expected to be invalid.", oAuthToken.isValid());
        assertTrue("Token is expected to be expired.", oAuthToken.isExpired());
    }

    @Test
    public void testTokenParsing_withNonBearerToken() {
        OAuthToken oAuthToken = cache.parseToken(TokenGenerator.getInstance().generateToken(clientId, 30L, "Refresh"));

        assertNotNull("Result from TokenToParsedTokenCache.parseToken(...) should never be null.", oAuthToken);
        assertNotNull("ClientId does not match.", oAuthToken.getClientId());
        assertFalse("Token is expected to be invalid.", oAuthToken.isValid());
        assertFalse("Token is expected to be expired.", oAuthToken.isExpired());
    }



}