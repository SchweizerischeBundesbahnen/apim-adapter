package ch.sbb.integration.api.gateway.proxy;

import io.undertow.server.handlers.proxy.ConnectionPoolErrorHandler;
import io.undertow.server.handlers.proxy.ConnectionPoolManager;

/**
 * Implementation of a simple connection pool manager.
 */
final class SimpleConnectionPoolManager extends ConnectionPoolErrorHandler.SimpleConnectionPoolErrorHandler implements ConnectionPoolManager {
    private final ProxyConfig proxyConfig;

    public SimpleConnectionPoolManager(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override // from ConnectionPoolManager
    public int getProblemServerRetry() {
        return proxyConfig.getProblemServerRetry();
    }

    @Override // from ConnectionPoolManager
    public int getMaxConnections() {
        return proxyConfig.getConnectionsPerThread();
    }

    @Override // from ConnectionPoolManager
    public int getMaxCachedConnections() {
        return proxyConfig.getMaxCachedConnections();
    }

    @Override // from ConnectionPoolManager
    public int getSMaxConnections() {
        return proxyConfig.getSoftMaxConnections();
    }

    @Override // from ConnectionPoolManager
    public long getTtl() {
        return proxyConfig.getTtl();
    }

    @Override // from ConnectionPoolManager
    public int getMaxQueueSize() {
        return proxyConfig.getMaxQueueSize();
    }
}
