package ch.sbb.integration.api.gateway.handler;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_4005;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5010;

public class CorrelationHandler implements MiddlewareHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationHandler.class);
    private static final String CONFIG_NAME = "correlation";
    //also change in the logback.xml twice (also in the Docker dir)
    private static final String CORRELATION_LOG_PROPERTY_NAME = "X-B3-TraceId";
    private static final String BUSINESS_LOG_PROPERTY_NAME = "X-Business-Id";

    private volatile HttpHandler next;

    private static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private static final boolean ENABLED = isCorrelationEnabled();
    private static final String CORRELATION_HEADER_NAME = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("CorrelationIdHeaderName");
    private static final String BUSINESS_HEADER_NAME = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("BusinessIdHeaderName");


    private final HttpString correlationIdAsHttpString;

    public CorrelationHandler() {
        LOG.info(APIM_4005.pattern(), CORRELATION_HEADER_NAME, BUSINESS_HEADER_NAME);
        this.correlationIdAsHttpString = new HttpString(CORRELATION_HEADER_NAME);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        String cId = exchange.getRequestHeaders().getFirst(CORRELATION_HEADER_NAME);
        if (cId == null) {
            cId = UUID.randomUUID().toString();
            exchange.getRequestHeaders().put(correlationIdAsHttpString, cId);
        }
        MDC.put(CORRELATION_LOG_PROPERTY_NAME, cId);

        String bId = exchange.getRequestHeaders().getFirst(BUSINESS_HEADER_NAME);
        if (bId != null) {
            MDC.put(BUSINESS_LOG_PROPERTY_NAME, bId);
        }

        MDC.clear();
        Handler.next(exchange);
        MDC.clear();
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler httpHandler) {
        Handlers.handlerNotNull(httpHandler);
        this.next = httpHandler;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return ENABLED;
    }


    private static boolean isCorrelationEnabled() {
        Object object = config.get("enableCorrelation");
        if (object instanceof Boolean) {
            return (Boolean) object;
        }
        if (object instanceof String) {
            return Boolean.parseBoolean((String) object);
        }
        return false;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(
                CorrelationHandler.class.getName(),
                Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME),
                null);
    }

    public static void setupMDC(HttpServerExchange exchange) {
        if (ENABLED) {
            if (MDC.get(CORRELATION_LOG_PROPERTY_NAME) != null || MDC.get(BUSINESS_LOG_PROPERTY_NAME) != null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(APIM_5010.pattern());
                }
            }

            MDC.put(BUSINESS_LOG_PROPERTY_NAME, exchange.getRequestHeaders().getFirst(CorrelationHandler.BUSINESS_HEADER_NAME));
            MDC.put(CORRELATION_LOG_PROPERTY_NAME, exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID));
        }
    }

}
