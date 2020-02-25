package ch.sbb.integration.api.adapter.config;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Test;

import java.security.KeyStoreException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RestConfigTest {
    @Test
    public void testNewRestEasyClient() {
        RestConfig restConfig = new RestConfig();
        ResteasyClient resteasyClient = restConfig.newRestEasyClient();
        assertNotNull(resteasyClient);
    }


    @Test
    public void testCustomTrustStoreInvalidPassword() throws KeyStoreException {
        String truststore = RestConfigTest.class.getResource("/truststore.jks").getFile();
        System.setProperty("javax.net.ssl.trustStore", truststore);
        System.setProperty("javax.net.ssl.trustStorePassword", "wrongPw");
        RestConfig restConfig = new RestConfig();

        ResteasyClient resteasyClient = restConfig.newRestEasyClient();
        assertNotNull(resteasyClient);
        assertNull(resteasyClient.getSslContext());
    }

    @Test
    public void testCustomTrustStoreInvalidStore() throws KeyStoreException {
        System.setProperty("javax.net.ssl.trustStore", "noValidTrust");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        RestConfig restConfig = new RestConfig();

        ResteasyClient resteasyClient = restConfig.newRestEasyClient();
        assertNotNull(resteasyClient);
        assertNull(resteasyClient.getSslContext());
    }

    @Test
    public void testNewRestEasyClientWithCustomProxy() {
        System.setProperty("https.proxyHost", "zscaler.sbb.ch");
        System.setProperty("https.proxyPort", "9400");
        RestConfig restConfig = new RestConfig();

        ResteasyClient resteasyClient = restConfig.newRestEasyClient();
        assertNotNull(resteasyClient);
    }
}