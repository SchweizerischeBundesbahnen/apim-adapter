package ch.sbb.integration.api.adapter.model;

import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuer;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OAuthTokenTest {
    @Test
    public void testNull() {
        final OAuthToken oAuthToken = new OAuthToken(null, null, null, null, null, null);
        assertEquals(OAuthToken.TokenStateEnum.INVALID, oAuthToken.getTokenState());
        assertFalse(oAuthToken.isValid());
        assertTrue(oAuthToken.isExpired());
        assertFalse(oAuthToken.isBearer());
        assertNull(oAuthToken.getClientId());
        assertNull(oAuthToken.getExpiration());
        assertNull(oAuthToken.getToken());
        assertNull(oAuthToken.getType());
        assertNull(oAuthToken.getTokenIssuer());
    }

    @Test
    public void testValidAndExpired() throws InterruptedException {
        final String clientId = "abcd";
        final long ttlSeconds = 2L;
        final String token = TokenGenerator.getInstance().generateBearerToken(clientId, ttlSeconds);
        final Long expirationMillis = System.currentTimeMillis() + ttlSeconds * 1_000;
        final OAuthToken oAuthToken = new OAuthToken(OAuthToken.TokenStateEnum.VALID, new TokenIssuer(null, null), token, clientId, expirationMillis / 1000, "Bearer");
        assertEquals(OAuthToken.TokenStateEnum.VALID, oAuthToken.getTokenState());
        assertTrue(oAuthToken.isValid());
        assertFalse(oAuthToken.isExpired());
        assertTrue(oAuthToken.isBearer());
        assertEquals(clientId, oAuthToken.getClientId());
        assertTrue(Math.abs(expirationMillis - oAuthToken.getExpiration()) < 1000);
        assertEquals(token, oAuthToken.getToken());
        assertEquals("Bearer", oAuthToken.getType());
        assertNotNull(oAuthToken.getTokenIssuer());

        // wait in order to let the token to become expired
        TimeUnit.MILLISECONDS.sleep((ttlSeconds * 1000) + 1);

        // now token has to be expired and invalid
        assertEquals(OAuthToken.TokenStateEnum.EXPIRED, oAuthToken.getTokenState());
        assertFalse(oAuthToken.isValid());
        assertTrue(oAuthToken.isExpired());
    }

}