package ch.sbb.integration.api.gateway.proxy;

import io.prometheus.client.CollectorRegistry;
import io.undertow.server.HttpServerExchange;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class PooledProxyClientRetryTest {

    private PooledProxyClient pooledProxyClient;
    private HttpServerExchange exchange;

    @Before
    public void init() {
        // make sure registry is empty before execution, otherwise multiple test executions collide with "Collector already registered"
        CollectorRegistry.defaultRegistry.clear();

        final ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setRules(new ArrayList<>());
        /*
         * location: ^/a/b/(.*)$
         * rewrite:  $1
         * proxyPass http://backend/x/y
         */
        final ProxyRule rule = new ProxyRule();
        rule.setLocation("^/a/b/(.*)$");
        rule.setRewrite("$1");
        rule.setProxyPass("http://backend/x/y");
        proxyConfig.getRules().add(rule);

        pooledProxyClient = new PooledProxyClient(proxyConfig);
        exchange = new HttpServerExchange(null);
    }

    /**
     * Origin: <a href="https://issues.sbb.ch/browse/AITG-635">AITG-635</a>
     */
    @Test
    public void testRepeatedTargetHostBasePathUriInvocation() {
        exchange.setRequestURI("/a/b/foo/bar");

        final URI expectedTargetHostBasePathUri = URI.create("http://backend/x/y");

        // request URI is unmodified here
        assertEquals("/a/b/foo/bar", exchange.getRequestURI());

        // "effectiveTargetHostBasePath" has a side effect on the exchange object, in case there are proxy rules - it may alter the requestUri
        // the following makes sure that the method can be invoked multiple times with re-modifying the request uri
        final URI effectiveTargetHostBasePathUri = pooledProxyClient.getEffectiveTargetHostBasePathUri(exchange);
        assertEquals(expectedTargetHostBasePathUri, effectiveTargetHostBasePathUri);
        assertEquals(expectedTargetHostBasePathUri, pooledProxyClient.getEffectiveTargetHostBasePathUri(exchange));
        assertEquals(pooledProxyClient.getEffectiveTargetHostBasePathUri(exchange), pooledProxyClient.getEffectiveTargetHostBasePathUri(exchange));

        // the modified request URI
        assertEquals("/foo/bar", exchange.getRequestURI());
    }
}