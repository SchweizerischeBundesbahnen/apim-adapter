package ch.sbb.integration.api.gateway;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.config.util.check.ConnectionCheck;
import ch.sbb.integration.api.adapter.config.util.check.PublicKeyCheck;
import ch.sbb.integration.api.adapter.config.util.check.SyncCheck;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.configuration.OperationMode;
import ch.sbb.integration.api.adapter.service.monitoring.MonitoringService;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import com.networknt.config.Config;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by u217269 on 19.04.2018.
 */
public final class ApimSingleton {

    private static ApimAdapterService service;
    private static Undertow adminServer;
    private static ApimAdapterConfig apimAdapterConfig;

    private static final String CONFIG_NAME = "apim";

    private static Boolean apimFilterEnabled;

    private static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

    private ApimSingleton() {
        // Hide Constructor for Singleton.
    }

    public static synchronized ApimAdapterService get() {
        if (service == null) {
            apimFilterEnabled = (Boolean) config.get("enableApiManagement");
            service = ApimAdapterFactory.createApimAdapterService(getAdapterConfig(), OperationMode.GATEWAY, apimFilterEnabled);
        }
        return service;
    }

    public static synchronized void set(ApimAdapterService newService) {
        service = newService;
    }

    public static Undertow getAdminServer() {
        return adminServer;
    }

    public static void setAdminServer(Undertow adminServer) {
        ApimSingleton.adminServer = adminServer;
    }

    public static ApimAdapterConfig getAdapterConfig() {
        if (apimAdapterConfig == null) {
            apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        }
        return apimAdapterConfig;
    }

    public static void setAdapterConfig(ApimAdapterConfig config) {
        apimAdapterConfig = config;
    }

    public static Boolean isApimFilterEnabled() {
        return apimFilterEnabled;
    }
}
