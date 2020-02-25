package ch.sbb.integration.api.gateway.proxy;

import com.networknt.config.Config;
import com.networknt.handler.HandlerProvider;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;

public class ProxyHandlerProvider implements HandlerProvider {

    private static final String CONFIG_NAME = "proxy";
    private static ProxyConfig config = (ProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ProxyConfig.class);

    @Override
    public HttpHandler getHandler() {
        return ProxyHandler.builder()
                .setProxyClient(new PooledProxyClient(config))
                .setMaxRequestTime(config.getMaxRequestTime())
                .setNext(ResponseCodeHandler.HANDLE_404)
                .setRewriteHostHeader(config.isRewriteHostHeader())
                .setReuseXForwarded(false)
                .setMaxConnectionRetries(config.getMaxConnectionRetries() == null ? 1 : config.getMaxConnectionRetries())
                .build();
    }
}
