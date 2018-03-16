package ch.sbb.integration.api.threescale.service.utils;

import ch.sbb.integration.api.threescale.model.OAuthToken;
import ch.sbb.integration.api.threescale.service.cache.TokenToParsedTokenCache;
import ch.sbb.integration.api.threescale.util.TokenGenerator;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by u217269 on 01.03.2018.
 */
public class TokenGeneratorTest {

    @Test
    public void testGenerationOf_validToken() {
        String token = TokenGenerator.generateValidToken("test-client", 30_000);
        OAuthToken oAuthToken = TokenToParsedTokenCache.extractClientId(token);
        assertFalse("Token should not be expired", oAuthToken.isExpired());
        assertTrue("Token is expected to be valid.", oAuthToken.isValid());
    }

}