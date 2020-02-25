package ch.sbb.integration.api.adapter.model.tokenissuer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TokenIssuerStoreTest {

    @Test
    public void guessTokenIssuerUrls() {
        assertNull(TokenIssuerStore.guessTokenIssuerUrls(null).get(0));
        assertEquals("", TokenIssuerStore.guessTokenIssuerUrls("").get(0));

        final List<String> one = TokenIssuerStore.guessTokenIssuerUrls("https://sso-dev.sbb.ch/auth/realms/(SBB_Public)");
        assertEquals(1, one.size());
        assertEquals("https://sso-dev.sbb.ch/auth/realms/SBB_Public", one.get(0));

        final List<String> oneWithoutGroup = TokenIssuerStore.guessTokenIssuerUrls("https://sso-dev.sbb.ch/auth/realms/SBB_Public");
        assertEquals(1, oneWithoutGroup.size());
        assertEquals("https://sso-dev.sbb.ch/auth/realms/SBB_Public", oneWithoutGroup.get(0));

        final List<String> three = TokenIssuerStore.guessTokenIssuerUrls("https://sso-dev.sbb.ch/auth/realms/(SBB_Public|SBB_Public2|SBB_Public3)");
        assertEquals(3, three.size());
        assertEquals("https://sso-dev.sbb.ch/auth/realms/SBB_Public", three.get(0));
        assertEquals("https://sso-dev.sbb.ch/auth/realms/SBB_Public2", three.get(1));
        assertEquals("https://sso-dev.sbb.ch/auth/realms/SBB_Public3", three.get(2));

        final List<String> oneEmptyGroup = TokenIssuerStore.guessTokenIssuerUrls("https://www-etsnap2.swisspass.ch/v2/oevlogin/oauth2/()");
        assertEquals(1, oneEmptyGroup.size());
        assertEquals("https://www-etsnap2.swisspass.ch/v2/oevlogin/oauth2/", oneEmptyGroup.get(0));
    }

    @Test
    public void containsRegex() {
        assertTrue(TokenIssuerStore.containsRegex(".*"));
        assertTrue(TokenIssuerStore.containsRegex(".+"));
        assertTrue(TokenIssuerStore.containsRegex("[a-Z]"));
        assertTrue(TokenIssuerStore.containsRegex("a[a-Z]*b"));
        assertTrue(TokenIssuerStore.containsRegex("a{1,}"));
        assertTrue(TokenIssuerStore.containsRegex("\\w"));
        assertFalse(TokenIssuerStore.containsRegex(null));
        assertFalse(TokenIssuerStore.containsRegex(""));
        assertFalse(TokenIssuerStore.containsRegex("abc"));
        assertFalse(TokenIssuerStore.containsRegex("abc(def)ghe"));
    }
}