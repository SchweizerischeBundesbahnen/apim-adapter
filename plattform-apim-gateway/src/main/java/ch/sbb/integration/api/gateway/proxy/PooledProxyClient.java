package ch.sbb.integration.api.gateway.proxy;

import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.service.monitoring.ManifestCollector;
import ch.sbb.integration.api.adapter.service.monitoring.ManifestExtractor;
import ch.sbb.integration.api.gateway.ApimSingleton;
import ch.sbb.integration.api.gateway.handler.CorrelationHandler;
import ch.sbb.integration.api.gateway.handler.LoggingExchangeCompletionListener;
import ch.sbb.integration.api.gateway.monitoring.GatewayMetricsCollector;
import com.networknt.client.Http2Client;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.handlers.proxy.ProxyConnectionPool;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;

/**
 * Start the PooledProxyClient (reverse proxy) with a list of rewrite rules the proxy. PooledProxyClient uses
 * connection pools.
 * <p>
 * Example:
 * Request 1: localhost:8080/foo -&gt; http://default-domain.com/foo
 * Request 2: localhost:8080/api/bar -&gt; http://some-domain.com/bar
 *
 * @author u223622
 */
public class PooledProxyClient implements ProxyClient {

    public static final AttachmentKey<String> INITIAL_REQUEST_URI = AttachmentKey.create(String.class);
    public static final AttachmentKey<URI> TARGET_HOST_BASE_PATH_URI = AttachmentKey.create(URI.class);
    // used by vnext apim gateway
    public static final AttachmentKey<String> TARGET_REQUEST_URI = AttachmentKey.create(String.class);

    private static final Logger LOG = LoggerFactory.getLogger(PooledProxyClient.class);
    private static final String APIM_GATEWAY_MANIFEST = "plattform-apim-gateway";
    private static final ProxyTarget TARGET = new ProxyTarget() {
    };

    private final ProxyConfig proxyConfig;
    private final UndertowClient client = UndertowClient.getInstance();
    private final ConcurrentHashMap<URI, ProxyConnectionPool> proxyConnectionPoolMap = new ConcurrentHashMap<>();

    private final Timer timer = new Timer("SelfUpdatingPooledProxyClient");
    private volatile URI defaultUri = null;

    public PooledProxyClient(ProxyConfig proxyConfig) {
        if (proxyConfig == null) {
            LOG.error(APIM_6001.pattern());
            throw new NullPointerException("proxyConfig");
        }

        this.proxyConfig = proxyConfig;

        LOG.info("Going to register gateway metrics collector");
        new GatewayMetricsCollector(proxyConnectionPoolMap).register();
        new ManifestCollector(new ManifestExtractor(), APIM_GATEWAY_MANIFEST).register();

        // Enable scheduled updates of target host
        long period = 1_000L * 15;
        long delay = period;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    defaultUri = new URI(ApimSingleton.get().getApiProxy().getTargetUrl());
                    LOG.debug("Loaded target host from API-Management: '{}'", defaultUri);
                } catch (URISyntaxException e) {
                    LOG.error(APIM_6002.pattern(), e);
                    throw new RuntimeException(e);
                }
            }
        }, delay, period);
    }

    /**
     * Replace back reference in replacement string with match group values from matcher. If no match groups
     * were found, replacement string is returned.
     *
     * @param matcher     The regex matcher.
     * @param replacement The replacement string containing optional back references.
     * @return A string with replaced backrefs.
     */
    private static String replaceBrefsInReplacement(Matcher matcher, String replacement) {
        if (matcher.groupCount() == 0) {
            return replacement;
        } else {
            return matcher.replaceFirst(replacement);
        }
    }

    @Override // from ProxyClient
    public ProxyTarget findTarget(HttpServerExchange exchange) {
        return TARGET;
    }

    @Override // from ProxyClient
    public void getConnection(ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
        CorrelationHandler.setupMDC(exchange);
        storeAndLogInitialRequestUri(exchange);


        final LoggingProxyCallback loggingProxyCallback = new LoggingProxyCallback(callback);

        final URI targetHostBasePathUri = getEffectiveTargetHostBasePathUri(exchange);
        if (targetHostBasePathUri == null) {
            LOG.error(APIM_6003.pattern());
            loggingProxyCallback.couldNotResolveBackend(exchange);
            return;
        }

        // Get connection pool for target host base URI and delegate the (rewritten) request
        try {
            exchange.putAttachment(LoggingExchangeCompletionListener.PROXY_HANDLE_START_TIME, System.currentTimeMillis());
            MDC.clear();
            getConnectionPool(targetHostBasePathUri).connect(target, exchange, loggingProxyCallback, timeout, timeUnit, false);
        } catch (UnresolvedAddressException e) {
            // Happened in AITG-591
            // track UnresolvedAddressException explicitly
            ProxyStatistics.get().incrementUnresolvedAddressException();
            // however still trigger "couldNotResolveBackend" as this closes the client connection, which otherwise hangs until timeout (like 10sec)
            loggingProxyCallback.couldNotResolveBackend(exchange);
            throw e; // rethrow
        }
    }


    private void storeAndLogInitialRequestUri(HttpServerExchange exchange) {
        // store the initial / origin unmodified request URI (see AITG-635)
        String initialRequestUri = exchange.getAttachment(INITIAL_REQUEST_URI);
        if (initialRequestUri == null) {
            initialRequestUri = exchange.getRequestURI();
            exchange.putAttachment(INITIAL_REQUEST_URI, initialRequestUri);
        }
        LOG.debug("Got request location: {}", initialRequestUri);
    }

    /**
     * @param exchange
     * @return may return e.g. http://backend-service:80 or http://backend-service:80/foo/bar
     */
    URI getEffectiveTargetHostBasePathUri(HttpServerExchange exchange) {
        final URI existingTargetHostBasePathUri = exchange.getAttachment(TARGET_HOST_BASE_PATH_URI);
        if (existingTargetHostBasePathUri != null) {
            // if already set, then we assume this is a retry
            ProxyStatistics.get().incrementRetries();
            LOG.debug("TARGET_HOST_BASE_PATH_URI already set to URI={}", existingTargetHostBasePathUri);
            return existingTargetHostBasePathUri;
        }

        // Set default target host URI
        URI targetHostBasePathUri = defaultUri;

        if (proxyConfig.getRules() != null) {
            targetHostBasePathUri = processProxyRules(exchange, targetHostBasePathUri);
        }

        if (targetHostBasePathUri == null) {
            LOG.warn(APIM_5001.pattern());
            Proxy apiProxy = ApimSingleton.get().getApiProxy();
            if (apiProxy != null) {
                LOG.warn(APIM_5002.pattern(), apiProxy.getTargetUrl());
                try {
                    targetHostBasePathUri = new URI(apiProxy.getTargetUrl());
                } catch (URISyntaxException e) {
                    LOG.error(APIM_6004.pattern(), e);
                }
            }
            LOG.error(APIM_6005.pattern(), targetHostBasePathUri, proxyConfig.getRules(), defaultUri, exchange.getRequestURI());
        }

        exchange.putAttachment(TARGET_HOST_BASE_PATH_URI, targetHostBasePathUri);
        exchange.putAttachment(TARGET_REQUEST_URI, exchange.getRequestURI());

        LOG.debug("targetHostBasePath={} targetRequestUri={}", targetHostBasePathUri, exchange.getRequestURI());

        return targetHostBasePathUri;
    }

    private ProxyConnectionPool getConnectionPool(final URI targetHostBasePathUri) {
        return proxyConnectionPoolMap.computeIfAbsent(targetHostBasePathUri, k -> {
            final boolean isSecure = targetHostBasePathUri.getScheme().equalsIgnoreCase("https");
            return new ProxyConnectionPool(new SimpleConnectionPoolManager(proxyConfig),
                    null, targetHostBasePathUri, isSecure ? Http2Client.SSL : null, client, OptionMap.EMPTY);
        });
    }

    /**
     * Processes the configured proxy rules and applies rewriting of backend location and/or backend host.
     *
     * @param exchange              The current exchange.
     * @param targetHostBasePathUri The default URI of the target base path.
     * @return The URI with the rewritten target host and/or location.
     */
    URI processProxyRules(HttpServerExchange exchange, URI targetHostBasePathUri) {
        for (ProxyRule rule : proxyConfig.getRules()) {
            if (rule.getLocationPattern() == null) {
                // Invalid rule
                LOG.warn(APIM_5003.pattern());
                continue;
            }

            final Matcher matcher = rule.getLocationPattern().matcher(exchange.getRequestURI());
            if (!matcher.find()) {
                // Proceed to next rule
                continue;
            }

            // Compute target host URI (host, port and protocol)
            final String proxyPass = rule.getProxyPass();
            if (proxyPass != null && !proxyPass.trim().isEmpty()) {
                LOG.debug("overriding targetHostBasePathUri from '{}'", targetHostBasePathUri);
                targetHostBasePathUri = URI.create(proxyPass);
                LOG.debug("result of override '{}'", targetHostBasePathUri);
            }

            // Compute location, ensure that it starts with a slash
            if (rule.getRewrite() != null) {
                final String targetLocation = replaceBrefsInReplacement(matcher, rule.getRewrite());
                if (targetLocation.startsWith("/")) {
                    exchange.setRequestURI(targetLocation);
                } else {
                    exchange.setRequestURI("/" + targetLocation);
                }
            }

            // Log and break
            LOG.debug("Location match rewritten pass to: {}  location: {} ", targetHostBasePathUri, exchange.getRequestURI());
            break;
        }
        return targetHostBasePathUri;
    }

}