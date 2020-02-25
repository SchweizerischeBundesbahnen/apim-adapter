package ch.sbb.integration.api.gateway.proxy;

import java.util.List;

/**
 * Config class for reverse proxy.
 *
 * @author u223622
 */
public class ProxyConfig {
    private boolean enabled;
    private int connectionsPerThread;
    private int softMaxConnections;
    private int maxCachedConnections;
    private int maxQueueSize;
    private int ttl;
    private int problemServerRetry;
    private int maxRequestTime;
    private boolean rewriteHostHeader;
    private List<ProxyRule> rules;
    private Integer maxConnectionRetries;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }

    public void setConnectionsPerThread(int connectionsPerThread) {
        this.connectionsPerThread = connectionsPerThread;
    }

    public int getSoftMaxConnections() {
        return softMaxConnections;
    }

    public void setSoftMaxConnections(int softMaxConnections) {
        this.softMaxConnections = softMaxConnections;
    }

    public int getMaxCachedConnections() {
        return maxCachedConnections;
    }

    public void setMaxCachedConnections(int maxCachedConnections) {
        this.maxCachedConnections = maxCachedConnections;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public int getProblemServerRetry() {
        return problemServerRetry;
    }

    public void setProblemServerRetry(int problemServerRetry) {
        this.problemServerRetry = problemServerRetry;
    }

    public int getMaxRequestTime() {
        return maxRequestTime;
    }

    public void setMaxRequestTime(int maxRequestTime) {
        this.maxRequestTime = maxRequestTime;
    }

    public boolean isRewriteHostHeader() {
        return rewriteHostHeader;
    }

    public void setRewriteHostHeader(boolean rewriteHostHeader) {
        this.rewriteHostHeader = rewriteHostHeader;
    }

    public List<ProxyRule> getRules() {
        return rules;
    }

    public void setRules(List<ProxyRule> rules) {
        this.rules = rules;
    }

    public Integer getMaxConnectionRetries() {
        return maxConnectionRetries;
    }

    public void setMaxConnectionRetries(Integer maxConnectionRetries) {
        this.maxConnectionRetries = maxConnectionRetries;
    }
}


