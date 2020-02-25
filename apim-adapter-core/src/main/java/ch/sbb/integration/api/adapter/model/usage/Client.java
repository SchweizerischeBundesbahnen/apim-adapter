package ch.sbb.integration.api.adapter.model.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_3004;

/**
 * Created by u217269 on 16.02.2018.
 */
public class Client implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
    private static final long serialVersionUID = -2265193224419120561L;

    private final String id;
    private ConcurrentHashMap<String, MetricUsage> usageMap;
    private ClientSyncState syncState;

    public Client(String id, Map<String, MetricUsage> usageMap, ClientSyncState syncState) {
        this.id = id;
        this.usageMap = new ConcurrentHashMap<>(usageMap); //Defensive copy
        this.syncState = syncState;
    }

    public boolean isAppWithPermission() {
        return ClientSyncState.isStatePermitted(syncState);
    }

    public void updateMetricUsage(String metricSysName, Function<? super MetricUsage, ? extends MetricUsage> metricUsageTransformation) {
        usageMap.compute(metricSysName,(key, metricUsage) -> metricUsageTransformation.apply(metricUsage));
    }

    public List<String> getMetricSysNames() {
        return Collections.list(usageMap.keys());
    }

    public void deleteNoneMatchingMetrics(List<String> knownMetricSysNames) {
        ArrayList<String> metricsToDelete = Collections.list(usageMap.keys());
        knownMetricSysNames.forEach(metricsToDelete::remove);
        metricsToDelete.forEach(obsoleteMetricSysName -> usageMap.remove(obsoleteMetricSysName));
    }

    public void deleteMatchingMetrics(List<String> metricsToDelete) {
        metricsToDelete.forEach(obsoleteMetricSysName -> usageMap.remove(obsoleteMetricSysName));
    }

    public String getId() {
        return id;
    }

    public MetricUsage getUsage(String metricSysName) {
        return usageMap.get(metricSysName);
    }

    public ClientSyncState getSyncState() {
        return syncState;
    }

    public void setSyncState(ClientSyncState syncState) {
        this.syncState = syncState;
    }

    public boolean incrementUsage(String metricSysName) {
        //Ensure that the metricUsage is in the correct time period
        updateMetricUsage(metricSysName, usage -> {
            //this is only possible when the clientCache is not synced (max cache_refresh_interval delay)
            if (usage == null) {
                return null;
            } else {
                return MetricUsage.getCurrentPeriod(usage);
            }
        });

        MetricUsage metricUsage = usageMap.get(metricSysName);
        if(metricUsage == null) {
            LOG.error(APIM_3004.pattern(), metricSysName, id);
            return true;
        } else {
            return metricUsage.incrementCurrentUsage(); // this increments the counter and returns if the access is allowed based on the current usage of this metric.
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Client client = (Client) o;

        return id.equals(client.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Client{" +
                "id='" + id + '\'' +
                ", usageMap=" + usageMap +
                ", syncState=" + syncState +
                '}';
    }

}
