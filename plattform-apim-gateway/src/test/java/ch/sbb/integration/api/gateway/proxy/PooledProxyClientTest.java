package ch.sbb.integration.api.gateway.proxy;

import io.prometheus.client.CollectorRegistry;
import io.undertow.server.HttpServerExchange;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class PooledProxyClientTest {

    private ProxyConfig proxyConfig;
    private PooledProxyClient pooledProxyClient;
    private HttpServerExchange exchange;

    @Before
    public void init() {
        // make sure registry is empty before execution, otherwise multiple test executions collide with "Collector already registered"
        CollectorRegistry.defaultRegistry.clear();

        proxyConfig = new ProxyConfig();
        proxyConfig.setRules(new ArrayList<>());
        pooledProxyClient = new PooledProxyClient(proxyConfig);
        exchange = new HttpServerExchange(null);
    }


    @Test
    public void emptyProxyRules() {
        /*
         * location: ^/foo/bar/(.*)/etc$
         * rewrite:  /new/foo/$1/bar
         * proxyPass http://backend:8080
         */
        final String originalRequestUri = "/foo/bar/baz/etc";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals(originalRequestUri, exchange.getRequestURI());
        assertEquals(originalTargetUri, rewrittenTargetUri);
    }

    @Test
    public void rewriteProxyRule() {
        /*
         * location: ^/foo/bar/(.*)/etc$
         * rewrite:  /new/foo/$1/bar
         */
        final ProxyRule rule = new ProxyRule();
        rule.setLocation("^/foo/bar/(.*)/etc$");
        rule.setRewrite("/new/foo/$1/bar");

        proxyConfig.getRules().add(rule);

        final String originalRequestUri = "/foo/bar/baz/etc";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals("/new/foo/baz/bar", exchange.getRequestURI());
        assertEquals(originalTargetUri, rewrittenTargetUri);
    }

    @Test
    public void rewriteRuleWithProxyPass() {
        /*
         * location: ^/foo/bar/(.*)/etc$
         * rewrite:  /new/foo/$1/bar
         * proxyPass http://backend:8080
         */
        final ProxyRule rule = new ProxyRule();
        rule.setLocation("^/foo/bar/(.*)/etc$");
        rule.setRewrite("/new/foo/$1/bar");
        rule.setProxyPass("http://backend:8080");

        proxyConfig.getRules().add(rule);

        final String originalRequestUri = "/foo/bar/baz/etc";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals("/new/foo/baz/bar", exchange.getRequestURI());
        assertEquals(URI.create("http://backend:8080"), rewrittenTargetUri);
    }

    @Test
    public void proxyPassOnly() {
        /*
         * location: ^/foo/bar/(.*)/etc$
         * proxyPass http://backend:8080
         */
        final ProxyRule rule = new ProxyRule();
        rule.setLocation("^/foo/bar/(.*)/etc$");
        rule.setProxyPass("http://backend:8080");

        proxyConfig.getRules().add(rule);

        final String originalRequestUri = "/foo/bar/baz/etc";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals(originalRequestUri, exchange.getRequestURI());
        assertEquals(URI.create("http://backend:8080"), rewrittenTargetUri);
    }


    @Test
    public void nonEndingLocationPatternProxyPassOnly() {
        /*
         * location: ^/novaan/nova-offline/verkauf/rest/(.*)/HeartBeatService
         * proxyPass http://novaan:8080
         */
        final ProxyRule rule = new ProxyRule();
        rule.setLocation("^/novaan/nova-offline/verkauf/rest/(.*)/HeartBeatService");
        rule.setProxyPass("http://novaan:8080");

        proxyConfig.getRules().add(rule);

        final String originalRequestUri = "/novaan/nova-offline/verkauf/rest/v1/HeartBeatService/registerDevice";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals(originalRequestUri, exchange.getRequestURI());
        assertEquals(URI.create("http://novaan:8080"), rewrittenTargetUri);
    }

    @Test
    public void restLocationPatternProxyPassOnly() {
        /*
         * location: /rest/
         * proxyPass http://novaan:8080
         */
        final ProxyRule rule = new ProxyRule();
        rule.setLocation("/rest/");
        rule.setProxyPass("http://backend:8080");

        proxyConfig.getRules().add(rule);

        final String originalRequestUri = "/random/rest/v1/Service/endpoint";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals(originalRequestUri, exchange.getRequestURI());
        assertEquals(URI.create("http://backend:8080"), rewrittenTargetUri);
    }

    @Test
    public void rewriteRuleWithProxyPassIncludingBasePath() {
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

        final String originalRequestUri = "/a/b/foo/bar";
        exchange.setRequestURI(originalRequestUri);
        final URI originalTargetUri = URI.create("https://backend-service-impl:8080");
        final URI rewrittenTargetUri = pooledProxyClient.processProxyRules(exchange, originalTargetUri);

        assertEquals("/foo/bar", exchange.getRequestURI());
        assertEquals(URI.create("http://backend/x/y"), rewrittenTargetUri);
    }

}