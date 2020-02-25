package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by u217269 on 10.04.2018.
 */
public class ClientCacheTest extends AbstractWiremockTest {

    @Before
    public void initStubs() {
        WireMock.reset();
        StubGenerator.instantiateStubForAuthorizeAndSync();
        StubGenerator.instantiateStubMappingRules();
        StubGenerator.instantiateStubForMetric();
    }

    @Test
    public void testReadingCurrentUsages() {
        ServiceToMetricsCache serviceToMetricsCache = new ServiceToMetricsCache(apimAdapterConfig, configurationLoader);
        ClientCache clientCache = new ClientCache(serviceToMetricsCache::get, apimAdapterConfig, configurationLoader);

        Client client = clientCache.get(CLIENT_ID);

        assertNotNull("Client must not be null", client);
        assertEquals("ClientId does not match.", CLIENT_ID, client.getId());
        assertEquals("Base does not match.", 3L, client.getUsage("hits").getBase().longValue());
        assertEquals("Count does not match.", 0L, client.getUsage("hits").getCurrentUsage().get());
    }

    
/*
 * TODO: write this test
    3Scale returns:
        <?xml version="1.0" encoding="UTF-8"?>
		<error code="application_not_found">application with id="084e8c300" was not found</error>
*/

}