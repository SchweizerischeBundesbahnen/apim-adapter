package ch.sbb.integration.api.gateway.monitoring;

import ch.sbb.integration.api.gateway.proxy.ProxyStatistics;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.undertow.server.handlers.proxy.ProxyConnectionPool;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.singletonList;

public class GatewayMetricsCollector extends Collector {

    private final ConcurrentMap<URI, ProxyConnectionPool> proxyConnectionPoolMap;

    public GatewayMetricsCollector(ConcurrentMap<URI, ProxyConnectionPool> proxyConnectionPoolMap) {
        this.proxyConnectionPoolMap = proxyConnectionPoolMap;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Arrays.asList(openConnections(), proxyStatistics());
    }

    private GaugeMetricFamily openConnections() {
        final List<String> openConnectionsLabels = singletonList("target");
        final GaugeMetricFamily openConnections = new GaugeMetricFamily("apim_gateway_proxy_connection_pool_open_connections", "The number of open proxy connections", openConnectionsLabels);

        proxyConnectionPoolMap.forEach((key, value) -> {
            final List<String> labelValues = singletonList(key.toString());
            openConnections.addMetric(labelValues, value.getOpenConnections());
        });
        return openConnections;
    }

    private GaugeMetricFamily proxyStatistics() {
        final GaugeMetricFamily proxyStatistics = new GaugeMetricFamily("apim_gateway_proxy_statistics", "Gateway wide proxy statistics. 'completed' is regular ok case, " +
                "'failed' are failures with different possible causes (like temporary connect issues) and counter may be increased more than once for a single call due to re-try handler (if configured), " +
                "'retries' are incremented every time the proxy re-tries a previously failed connection to the configured proxy target, " +
                "'queuedRequestFailed' happens when there is no proxy connection available for handling the request and queue is already full or disabled, " +
                "'unresolvableBackend' means connection to backend was not possible (various reason possible), " +
                "'unresolvedAddressException' is a special error variant of 'unresolvableBackend' in case DNS resolution of backend host fails - if this is greater than 0 check configuration and restart gateway (https://issues.jboss.org/browse/UNDERTOW-1611)", singletonList("value"));

        proxyStatistics.addMetric(singletonList("completed"), ProxyStatistics.get().getCompleted());
        proxyStatistics.addMetric(singletonList("retries"), ProxyStatistics.get().getRetries());
        proxyStatistics.addMetric(singletonList("failed"), ProxyStatistics.get().getFailed());
        proxyStatistics.addMetric(singletonList("queuedRequestFailed"), ProxyStatistics.get().getQueuedRequestFailed());
        proxyStatistics.addMetric(singletonList("unresolvableBackend"), ProxyStatistics.get().getUnresolvableBackend());
        proxyStatistics.addMetric(singletonList("unresolvedAddressException"), ProxyStatistics.get().getUnresolvedAddressException());

        return proxyStatistics;
    }

    @Override
    public <T extends Collector> T register() {
        return super.register();
    }
}
