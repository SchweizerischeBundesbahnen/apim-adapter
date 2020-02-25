package ch.sbb.integration.api.adapter.service.monitoring;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.MonitoringLevel;
import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.service.cache.Cache;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.job.ThreeScaleScheduler;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.hotspot.*;
import io.prometheus.jmx.JmxCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;

public class MonitoringService {

    private static final String JOBNAME = "Gateway_Monitoring_0.1";
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringService.class);
    private static final int INITIAL_DELAY = 15;
    public static final int TERMINATION_TIMEOUT = 20;
    private static final String APIM_ADAPTER_MANIFEST = "apim-adapter-core";

    private CacheMetricsCollector cacheMetricsCollector;
    private SimpleCacheSizeCollector simpleCacheSizeCollector;
    private ThreeScaleScheduler threeScaleScheduler;
    private PushGateway pushGateway;
    private ScheduledExecutorService scheduledExecutorService;
    private Map<String, String> groupingKey;
    private ApimAdapterConfig config;
    
    public MonitoringService(ApimAdapterConfig config, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
    	
    	this.config = config;

        if (config.isMonitoringDisabled()) {
            LOG.info(APIM_1022.pattern());
            return;
        }
        if (config.isMonitoringPushEnabled()) {
            LOG.info(APIM_1023.pattern());
            initPush();
        }

        new ConfigCollector(config, offlineConfigurationCacheRepo).register();
        new ManifestCollector(new ManifestExtractor(), APIM_ADAPTER_MANIFEST).register();

        if (MonitoringLevel.MINIMAL.equals(config.getMonitoringLevel())) {
            // init only cachesize monitoring
            initMinimalCollectors();
        }else if (MonitoringLevel.STANDARD.equals(config.getMonitoringLevel())) {
            // init thread, memory, GC, XNIO
            initMinimalCollectors();
            initStandardCollectors(Collections.singletonList("org.xnio:*"));

        }else {
            //not initializing minimal, because we create a different cacheCollector and don't need the simple one
            initStandardCollectors(Arrays.asList("org.xnio:*", "java.lang:type=GarbageCollector,name=PS*", "java.lang:type=Threading", "java.lang:type=MemoryPool"));
            initAllCollectors();
        }
    }

    private void initMinimalCollectors() {
        simpleCacheSizeCollector = new SimpleCacheSizeCollector().register();
    }

    private void initAllCollectors() {
        cacheMetricsCollector = new CacheMetricsCollector().register();
        new StandardExports().register();
        new VersionInfoExports().register();
    }

    private void initStandardCollectors(List<String> jmxBeans) {
        initJmxCollector(jmxBeans);
        new ThreadExports().register();
        new GarbageCollectorExports().register();
        new MemoryPoolsExports().register();
    }

    private void initJmxCollector(List<String> whitelistObjectNames) {
        JmxCollector jc = null;
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("lowercaseOutputName", true);
            settings.put("lowercaseOutputLabelNames", true);
            settings.put("whitelistObjectNames", whitelistObjectNames);
            jc = new JmxCollector(new Yaml().dump(settings));
        } catch (MalformedObjectNameException e) {
            LOG.warn(APIM_2019.pattern(), e);
        }
        if (jc != null) {
            jc.register();
        }
    }


    private void initPush() {
        try {
            pushGateway = new PushGateway(new URL(config.getMonitoringPushHost()));
        } catch (MalformedURLException e) {
            LOG.warn(APIM_2020.pattern(), e);
            return;
        }

        groupingKey = new HashMap<>();
        groupingKey.put("instance", config.getMonitoringId());
        groupingKey.put("namespace", config.getMonitoringNamespace());


        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(this::push, INITIAL_DELAY, config.getMonitoringPushIntervalInSeconds(), TimeUnit.SECONDS);
    }

    public void initializeSyncCollector(ThreeScaleScheduler threeScaleScheduler, EmergencyModeState emergencyModeState) {
        if (!config.isMonitoringDisabled()) {
            if (this.threeScaleScheduler == null) {
                this.threeScaleScheduler = threeScaleScheduler;
                new SyncCollector(threeScaleScheduler, emergencyModeState).register();
            }
        }
    }

    public void addCache(String s, Cache<?> cache) {
        if (!config.isMonitoringDisabled()) {
            if (!config.getMonitoringLevel().equals(MonitoringLevel.ALL)) {
                this.simpleCacheSizeCollector
                        .addCache(s, cache);
            } else {
                this.cacheMetricsCollector.addCache(s, cache.get());
            }
        }
    }

    private void push() {
        LOG.debug("Pushing Metrics to prometheus");
        try {
            pushGateway.push(CollectorRegistry.defaultRegistry, JOBNAME, groupingKey);
            LOG.debug("finished push to prometheus");
        } catch (IOException e) {
            LOG.info(ReasonCode.APIM_1030.pattern(), config.getMonitoringPushHost(), e);
        }
    }

    public void close() throws InterruptedException {
        LOG.info(APIM_1031.pattern());
        if (!config.isMonitoringDisabled() && scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
            try {
                pushGateway.delete(JOBNAME, groupingKey);
            } catch (IOException e) {
                LOG.info(APIM_1005.pattern(), e);
            }
        }
    }

}
