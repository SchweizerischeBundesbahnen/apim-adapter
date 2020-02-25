package ch.sbb.integration.api.gateway.handler;

import ch.sbb.integration.api.adapter.config.MonitoringLevel;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.service.utils.AuthUtils;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.gateway.ApimSingleton;
import com.networknt.audit.AuditHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_6006;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_6007;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_6008;

/**
 * Created by u217269 on 22.02.2018.
 */
public class ThreeScaleAdapterHandler implements MiddlewareHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleAdapterHandler.class);
    private static final String CONFIG_NAME = "apim";

    private static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private static final String CORS_REPLY_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ORIGIN_HEADER = "Origin";

    private final ApiWatchHandlerDelegate apiWatchHandlerDelegate;
    private volatile HttpHandler next;
    private Set<String> endpoints = Collections.synchronizedSet(new HashSet<>());

    public ThreeScaleAdapterHandler() {
        apiWatchHandlerDelegate = new ApiWatchHandlerDelegate();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        CorrelationHandler.setupMDC(exchange);
        try {
            tryHandleRequest(exchange);
        } catch (Exception e) {
            LOG.error(APIM_6006.pattern(), e);
            replyWithErrorMessage(
                    exchange,
                    "Unexpected Internal Server Error occurred",
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
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
        Object object = config.get("enableApiManagement");
        if (object instanceof Boolean) {
            return (Boolean) object;
        }
        if (object instanceof String) {
            return Boolean.parseBoolean((String) object);
        }
        return true;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(
                ThreeScaleAdapterHandler.class.getName(),
                Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME),
                null);
    }

    private void tryHandleRequest(HttpServerExchange exchange) throws Exception {
        exchange.putAttachment(LoggingExchangeCompletionListener.APIM_HANDLE_START_TIME, System.currentTimeMillis());

        final String path = exchange.getRequestPath();
        final String queryString = exchange.getQueryString();
        final String token = extractToken(exchange);
        final HttpMethod httpMethod = extractHttpMethod(exchange);

        if (ErrorResponseHelper.isNotMonitoringUrl(path)) {
            addPrometheusAuditInfo(exchange);
        }

        if (apiWatchHandlerDelegate.isApiWatchRequest(token, httpMethod)) {
            apiWatchHandlerDelegate.handleApiWatchRequest(exchange, token);
        } else if (ApimSingleton.getAdapterConfig().getExcludeFilterMethods().contains(httpMethod)) {
            MDC.clear();
            Handler.next(exchange);
        } else {
            applyApimFilter(exchange, path, queryString, token, httpMethod);
        }
    }

    private void applyApimFilter(HttpServerExchange exchange, String path, String queryString, String token, HttpMethod httpMethod) throws Exception {
        final AuthRepResponse authRepResponse = ApimSingleton.get().authRep(token, path, queryString, httpMethod);

        exchange.addExchangeCompleteListener(new ReportHitsExchangeCompletionListener(authRepResponse));
        exchange.addExchangeCompleteListener(new LoggingExchangeCompletionListener(authRepResponse));

        exchange.putAttachment(LoggingExchangeCompletionListener.APIM_HANDLE_END_TIME, System.currentTimeMillis());

        if (authRepResponse.isAllowed()) {
            MDC.clear();
            Handler.next(exchange);
        } else {
            if (authRepResponse.getWwwAuthenticateResponseHeader() != null) {
                exchange.getResponseHeaders().put(HttpString.tryFromString(AuthUtils.HTTP_WWW_AUTHENTICATE_HEADER_NAME), authRepResponse.getWwwAuthenticateResponseHeader());
            }

            replyWithErrorMessage(
                    exchange,
                    "Access not allowed: " + authRepResponse.getMessage(),
                    authRepResponse.getHttpStatus());
        }
    }

    private void addPrometheusAuditInfo(HttpServerExchange exchange) {
        final MonitoringLevel monitoringLevel = ApimSingleton.getAdapterConfig().getMonitoringLevel();
        if (EnumSet.of(MonitoringLevel.ALL, MonitoringLevel.STANDARD).contains(monitoringLevel)) {
            Map<String, Object> auditInfo = exchange.getAttachment(AuditHandler.AUDIT_INFO);
            // In normal case, the auditInfo shouldn't be null as it is created by SwaggerHandler with
            // endpoint and swaggerOperation available. This handler will enrich the auditInfo.
            if (auditInfo == null) {
                final String endpoint;
                if (endpoints.size() <= getEndpointLimit()) {
                    endpoint = exchange.getRequestPath();
                    endpoints.add(endpoint);
                } else {
                    endpoint = "<misc>";
                }

                auditInfo = new HashMap<>();
                auditInfo.put(Constants.CLIENT_ID_STRING, "*");
                auditInfo.put(Constants.ENDPOINT_STRING, endpoint);
                exchange.putAttachment(AuditHandler.AUDIT_INFO, auditInfo);
            }
        }
    }

    private int getEndpointLimit() {
        final Object monitoringEndpointLimit = config.get("monitoringEndpointLimit");
        if (monitoringEndpointLimit instanceof Number) {
            return ((Number) monitoringEndpointLimit).intValue();
        } else {
            return 20; // default
        }
    }

    private HttpMethod extractHttpMethod(HttpServerExchange exchange) {
        final HttpString requestMethod = exchange.getRequestMethod();
        try {
            if (requestMethod != null) {
                return HttpMethod.valueOf(requestMethod.toString().toUpperCase());
            } else {
                LOG.error(APIM_6007.pattern());
                return null;
            }
        } catch (Exception e) {
            LOG.error(APIM_6008.pattern(), requestMethod, e);
            return null;
        }
    }

    private String extractToken(HttpServerExchange exchange) {
        final HeaderValues authorizationHeader = exchange.getRequestHeaders().get(AuthUtils.HTTP_AUTHORIZATION_HEADER_NAME);
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            LOG.debug("Authorization header is missing.");
            return null;
        }
        return AuthUtils.extractJwtFromAuthHeader(authorizationHeader.getFirst());
    }

    private void replyWithErrorMessage(HttpServerExchange exchange, String statusMessage, int httpStatus) {
        exchange.setStatusCode(httpStatus);
        final HeaderValues originHeaders = exchange.getRequestHeaders().get(ORIGIN_HEADER);
        if (originHeaders != null && originHeaders.getFirst() != null && !originHeaders.getFirst().isEmpty()) {
            exchange.getResponseHeaders().add(new HttpString(CORS_REPLY_ALLOW_ORIGIN_HEADER), originHeaders.getFirst());
        }
        MDC.clear();
        exchange.getResponseSender().send(statusMessage);
    }
}
