package ch.sbb.integration.api.adapter.model.jwk;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JwksTest {

    @Test
    public void testRhSsoJwks() throws IOException, InvalidPublicKeyException {
        final String jwksJson = IOUtils.toString(JwksTest.class.getResourceAsStream("/jwks/rh-sso.json"), StandardCharsets.UTF_8);
        final Jwks jwks = new Jwks(jwksJson);
        verifyJwks(jwks, 3);
        verifyKey(jwks, "LDZ5q5LudDlCq0ZVof14Gm--AW4mvMMxUsvCMhcM62w");
    }

    @Test
    public void testAzureAdJwks() throws IOException, InvalidPublicKeyException {
        final String jwksJson = IOUtils.toString(JwksTest.class.getResourceAsStream("/jwks/azure-ad.json"), StandardCharsets.UTF_8);
        final Jwks jwks = new Jwks(jwksJson);
        verifyJwks(jwks, 3);
        verifyKey(jwks, "piVlloQDSMKxh1m2ygqGSVdgFpA");
        verifyKey(jwks, "HlC0R12skxNZ1WQwmjOF_6t_tDE");
        verifyKey(jwks, "M6pX7RHoraLsprfJeRCjSxuURhc");
    }

    @Test
    public void testSwisspass() throws IOException, InvalidPublicKeyException {
        final String jwksJson = IOUtils.toString(JwksTest.class.getResourceAsStream("/jwks/swisspass.json"), StandardCharsets.UTF_8);
        final Jwks jwks = new Jwks(jwksJson);
        verifyJwks(jwks, 1);
        verifyKey(jwks, "5dfad4b9");
    }

    private void verifyJwks(Jwks jwks, int nrOfKeys) {
        assertNotNull(jwks);
        assertNotNull(jwks.getKeys());
        assertEquals(nrOfKeys, jwks.getKeys().size());
        assertEquals(nrOfKeys, jwks.getKeyIds().size());
        assertFalse(jwks.getKey("does-not-exist").isPresent());
    }

    private void verifyKey(Jwks jwks, String keyId) throws InvalidPublicKeyException {
        assertNotNull(jwks.getKeys().get(keyId));
        assertNotNull(jwks.getKeys().get(keyId).getRsaPublicKey());
        assertTrue(jwks.getKey(keyId).isPresent());
    }

}
