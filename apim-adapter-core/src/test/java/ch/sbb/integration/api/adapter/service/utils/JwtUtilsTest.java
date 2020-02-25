package ch.sbb.integration.api.adapter.service.utils;

import ch.sbb.integration.api.adapter.util.TokenGenerator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JwtUtilsTest {

    @Test
    public void extractTokenFromAuthHeader() {
        assertNull(AuthUtils.extractJwtFromAuthHeader(null));
        assertEquals("", AuthUtils.extractJwtFromAuthHeader(""));
        assertEquals("", AuthUtils.extractJwtFromAuthHeader("   "));
        assertEquals("Bearer", AuthUtils.extractJwtFromAuthHeader("Bearer"));
        assertEquals("", AuthUtils.extractJwtFromAuthHeader("Bearer "));
        assertEquals("", AuthUtils.extractJwtFromAuthHeader("Bearer   "));
        assertEquals("a", AuthUtils.extractJwtFromAuthHeader("Bearer a "));
    }

    @Test
    public void mapAndExtract() {
        String bearerToken = TokenGenerator.getInstance().generateBearerToken("test-client", 42);
        assertEquals(bearerToken, AuthUtils.extractJwtFromAuthHeader(AuthUtils.mapJwtToHttpAuthorizationHeaderValue(bearerToken)));
    }

    @Test
    public void mapTokenToHttpAuthorizationHeaderValue() {
        assertEquals("Bearer ", AuthUtils.mapJwtToHttpAuthorizationHeaderValue(""));
        assertEquals("Bearer a", AuthUtils.mapJwtToHttpAuthorizationHeaderValue("a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapTokenToHttpAuthorizationHeaderValueNull() {
        AuthUtils.mapJwtToHttpAuthorizationHeaderValue(null);
    }
}