package ch.sbb.integration.api.threescale.service.cache;

import ch.sbb.integration.api.threescale.config.RestConfig;
import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import ch.sbb.integration.api.threescale.model.Proxy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by u217269 on 08.03.2018.
 */
public class ServiceToProxyCache {

    private final LoadingCache<String, Proxy> serviceToProxy;

    private final ResteasyClient rest;

    private final String threeScaleUrl;
    private final String threeScaleAdminUrl;
    private final String adminToken;

    public ServiceToProxyCache() {

        // ---------------------------
        // initialize instance
        // ---------------------------
        rest = RestConfig.newRestEasyClient();
        String protocol = ThreeScaleConfig.threeScaleHostUseHttps() ? "https" : "http";
        threeScaleUrl = protocol + "://" + ThreeScaleConfig.threeScaleHost();
        threeScaleAdminUrl = threeScaleUrl + "/admin/api";
        adminToken = ThreeScaleConfig.adminToken();

        // ---------------------------
        // initialize caches
        // ---------------------------
        serviceToProxy = Caffeine.newBuilder()
                .maximumSize(10_000)
                .refreshAfterWrite(ThreeScaleConfig.configReloadInSeconds(), TimeUnit.SECONDS)
                .build(serviceId -> loadProxySettings(serviceId));
    }

    /**
     * Public for testing.
     */
    public Proxy loadProxySettings(String serviceId) throws IOException {
        String url = threeScaleAdminUrl + "/services/" + serviceId + "/proxy/configs/production/latest.json?access_token=" + adminToken;
        String json = rest.target(url).request().get(String.class);
        JsonNode root = new ObjectMapper().readTree(json).get("proxy_config");
        String targetUrl = root.get("proxy").get("api_backend").asText();
        return new Proxy(targetUrl);
    }

    public Proxy get(String serviceId) {
        return serviceToProxy.get(serviceId);
    }

}
