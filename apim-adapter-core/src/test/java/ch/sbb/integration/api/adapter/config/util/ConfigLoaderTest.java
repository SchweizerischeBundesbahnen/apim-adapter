package ch.sbb.integration.api.adapter.config.util;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigLoaderTest {

    public static final String TOKEN = "c3fe8ec0a1144889edfbff2172ffa4kf954k3mcjdj298kjajkhdjekjasdj34laaaa";

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-junit-configloader.yml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInexistantException() {
        new ConfigLoader().getValueForProperty("does.not.exist", false);
    }

    @Test
    public void testOptionalProperty() {
        assertNull(new ConfigLoader().getValueForProperty("does.not.exist", true));
    }

    @Test
    public void testPropertyQualifierToPropertyName() {
        assertEquals("BACKEND_USEHTTPS", ConfigLoader.propertyQualifierToPropertyName("backend.useHttps"));
        assertEquals("APIM_BACKEND_USE_HTTPS", ConfigLoader.propertyQualifierToPropertyName("apim.backend.use-https"));
        assertEquals("APIM_BACKEND_USE_HTTPS", ConfigLoader.propertyQualifierToPropertyName("apim.backend.use_https"));
        assertEquals("APIM_BACKEND_USE_HTTPS", ConfigLoader.propertyQualifierToPropertyName("APIM_BACKEND_USE_HTTPS"));
        assertEquals("APIM_TOKENISSUER[]_URL_PATTERN", ConfigLoader.propertyQualifierToPropertyName("apim.tokenissuer[].url-pattern"));
    }

    @Test
    public void testPropertiedWithSpace() {
        System.setProperty(ConfigLoader.propertyQualifierToPropertyName("apim.junit.token.with.space"), TOKEN + " ");
        System.setProperty(ConfigLoader.propertyQualifierToPropertyName("apim.junit.token.with.leading.space"), " " + TOKEN);

        String result = new ConfigLoader().getValueForProperty("apim.junit.token.with.space", true);
        String resultLeading = new ConfigLoader().getValueForProperty("apim.junit.token.with.leading.space", true);

        assertEquals("Space at the end of the propertie should be removed", TOKEN, result);
        assertEquals("Space at the beginning of the propertie should be removed", TOKEN, resultLeading);
    }


    @Test
    public void testParseAsList() {
        assertTrue(ConfigLoader.parseAsList(null).isEmpty());
        assertTrue(ConfigLoader.parseAsList("").isEmpty());

        assertEquals(1, ConfigLoader.parseAsList("OPTIONS").size());
        assertEquals("OPTIONS", ConfigLoader.parseAsList("OPTIONS").get(0));

        assertEquals(2, ConfigLoader.parseAsList("OPTIONS,GET").size());
        assertEquals("OPTIONS", ConfigLoader.parseAsList("OPTIONS,GET").get(0));
        assertEquals("GET", ConfigLoader.parseAsList("OPTIONS,GET").get(1));
    }

    @Test
    public void testObjectListProperties() {
        final List<String> tokenIssuerUrlPatterns = new ConfigLoader().getValuesForPropertyList("apim.tokenissuer[].url-pattern");
        assertEquals(4, tokenIssuerUrlPatterns.size());
        assertEquals("http://localhost:8099/auth/realms/(SBB_Public)", tokenIssuerUrlPatterns.get(0));
        assertEquals("https://sso-dev.sbb.ch/auth/realms/(SBB_Public)", tokenIssuerUrlPatterns.get(1));
        assertEquals("https://login.microsoftonline.com/(93ead5cf-4825-45f3-9bc3-813cf64441af)/v2.0", tokenIssuerUrlPatterns.get(2));
        assertEquals("https://www-etsnap2.swisspass.ch/v2/oevlogin/oauth2/()", tokenIssuerUrlPatterns.get(3));
    }
}