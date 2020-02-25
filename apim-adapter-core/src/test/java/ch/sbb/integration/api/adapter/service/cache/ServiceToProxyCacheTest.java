package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by u217269 on 09.04.2018.
 */
public class ServiceToProxyCacheTest extends AbstractWiremockTest {

    @Before
    public void initStubs() {
        WireMock.reset();
        StubGenerator.instantiateStubForProxyRules();
    }

    @Test
    public void testReadingProxySettings() throws IOException, InterruptedException {
        ServiceToProxyCache serviceToProxyCache = new ServiceToProxyCache(apimAdapterConfig, configurationLoader);
        Proxy proxy = serviceToProxyCache.get(apimAdapterConfig.getAdapterServiceId());

        assertNotNull("Proxy must not be null", proxy);
        assertEquals("https://echo-api.3scale.net:443", proxy.getTargetUrl());
    }

}