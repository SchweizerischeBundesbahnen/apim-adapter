package ch.sbb.integration.api.adapter.service.monitoring;


import ch.sbb.integration.api.adapter.service.cache.Cache;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.*;

public class SimpleCacheSizeCollector extends Collector {

    private Map<String, Cache<?>> caches = new HashMap<>();

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();
        List<String> labelNames = Collections.singletonList("cache");
        GaugeMetricFamily cacheSize = new GaugeMetricFamily("caffeine_cache_estimated_size", "Estimated cache size", labelNames);
        mfs.add(cacheSize);

        caches.forEach((s, cache) ->
                cacheSize.addMetric(Collections.singletonList(s), (double) (cache.size()))
        );

        return mfs;
    }

    public void addCache(String s, Cache<?> cache) {
        caches.put(s, cache);
    }
}
