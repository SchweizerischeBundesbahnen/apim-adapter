package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.model.Proxy;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.cache.Cache;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_3002;

public final class CacheCheck {

    private static final String PROXY_CACHE_CHECK = "ProxyCacheCheck";

    private static final Logger LOG = LoggerFactory.getLogger(CacheCheck.class);

    private CacheCheck() {
        throw new IllegalStateException("Utility class");
    }

    public static CheckResult checkProxyCache(Cache<Proxy> cache, String serviceId) {
        try {
            cache.get(serviceId);
            return new CheckResult(PROXY_CACHE_CHECK, Status.UP, "Proxy found");
        } catch (ThreeScaleAdapterException e) {
            LOG.error(APIM_3002.pattern(), e);
            return new CheckResult(PROXY_CACHE_CHECK, Status.DOWN, "No proxy found");
        }
    }
}
