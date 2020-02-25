package ch.sbb.integration.api.gateway.hooks;

import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.gateway.ApimSingleton;
import ch.sbb.integration.api.gateway.handler.HealthHandler;
import ch.sbb.integration.api.gateway.handler.PrometheusHandler;
import ch.sbb.integration.api.gateway.handler.ReadinessHandler;
import ch.sbb.integration.api.gateway.handler.ShutdownHandler;
import com.networknt.config.Config;
import com.networknt.server.StartupHookProvider;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_4002;

public class AdminStartupHook implements StartupHookProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AdminStartupHook.class);

    private static final String CONFIG_NAME = "admin";

    private static final Map<String, Object> CONFIG = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

    @Override
    public void onStartup() {
        if(isEnabled()) {
            int port = (int) CONFIG.getOrDefault("httpPort", 3000);
            String ip = (String) CONFIG.getOrDefault("ip", "0.0.0.0");
            String healthMapping = (String) CONFIG.getOrDefault("healthMapping", "/health");
            String readinessMapping = (String) CONFIG.getOrDefault("readinessMapping", "/ready");
            String shutdownMapping = (String) CONFIG.getOrDefault("shutdownMapping", "/shutdown");
            String prometheusMapping = (String) CONFIG.getOrDefault("prometheusMapping", "/metrics");
            int ioThreads = (int) CONFIG.getOrDefault("ioThreads", 2);
            int workerThreads = (int) CONFIG.getOrDefault("workerThreads", ioThreads * 4);

            LOG.info(APIM_4002.pattern(), ip, port);

            final EmergencyModeState emergencyModeState = ApimSingleton.get().getEmergencyModeState();
            ReadinessHandler readinessHandler =  new ReadinessHandler(emergencyModeState);
            Undertow server = Undertow.builder()
                    .addHttpListener(port, ip)
                    .setHandler(new RoutingHandler()
                            .get(healthMapping, new HealthHandler(emergencyModeState))
                            .get(readinessMapping, readinessHandler)
                            .get(shutdownMapping, new ShutdownHandler(readinessHandler))
                            .get(prometheusMapping, new PrometheusHandler())
                    )
                    .setIoThreads(ioThreads)
                    .setWorkerThreads(workerThreads)
                    .build();
            server.start();
            ApimSingleton.setAdminServer(server);
        }else{
            LOG.info("Admin component is disabled in admin.yml");
        }
    }

    public boolean isEnabled() {
        Object object = CONFIG.get("enableAdmin");
        return object != null && (Boolean) object;
    }
}
