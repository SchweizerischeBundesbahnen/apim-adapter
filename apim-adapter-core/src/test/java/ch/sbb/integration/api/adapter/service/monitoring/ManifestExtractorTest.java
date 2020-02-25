package ch.sbb.integration.api.adapter.service.monitoring;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class ManifestExtractorTest {

    @Test
    public void testUnknownManifest() {
        //Act
        Map<String, String> stringStringMap = new ManifestExtractor().extractFromManifest("non-existing-manifest");

        //Assert
        assertThat(stringStringMap.size(), is(0));
    }

    @Test
    public void testKnownManifest() {
        //Act
        Map<String, String> stringStringMap = new ManifestExtractor().extractFromManifest("JUnit");

        //Assert
        assertThat(stringStringMap.size(), is(3));
        assertThat(stringStringMap.get("Implementation-Title"), is("JUnit"));
        assertThat(stringStringMap.get("Implementation-Version"), containsString("4."));
        assertThat(stringStringMap.get("Implementation-Vendor-Id"), is("junit"));
    }
}