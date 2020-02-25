package ch.sbb.integration.api.gateway.hooks;

import ch.sbb.integration.api.gateway.ApimSingleton;
import com.networknt.server.ShutdownHookProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreeScaleAdapterShutdownHook implements ShutdownHookProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleAdapterShutdownHook.class);

    @Override
    public void onShutdown() {
        LOG.info("Shutdown hook called");
        ApimSingleton.get().close();
        if(ApimSingleton.getAdminServer() != null){
            ApimSingleton.getAdminServer().stop();
        }
        LOG.info("Shutdown hook finished");
    }
}
