package ch.sbb.integration.api.threescale.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by u217269 on 16.02.2018.
 */
public class ClientStats implements Serializable {

    private String clientId;

    private Map<String, MetricUsage> usageMap;

    public ClientStats(String clientId) {
        this.clientId = clientId;
        this.usageMap = new ConcurrentHashMap<>();
    }

    public String getClientId() {
        return clientId;
    }

    public void putUsage(String metricName, MetricUsage usage) {
        usageMap.put(metricName, usage);
    }

    public MetricUsage getUsage(String metricName) {
        return usageMap.get(metricName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientStats clientStats = (ClientStats) o;

        return clientId.equals(clientStats.clientId);
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }

    public Collection<MetricUsage> usages() {
        return usageMap.values();
    }

    @Override
    public String toString() {
        return "ClientStats{" +
                "clientId='" + clientId + '\'' +
                ", usageMap=" + usageMap +
                '}';
    }

}
