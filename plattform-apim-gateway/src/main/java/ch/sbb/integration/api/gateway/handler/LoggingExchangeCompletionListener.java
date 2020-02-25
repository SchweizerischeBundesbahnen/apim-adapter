package ch.sbb.integration.api.gateway.handler;

import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import ch.sbb.integration.api.gateway.proxy.LoggingProxyCallback;
import ch.sbb.integration.api.gateway.proxy.ProxyExchangeStatus;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_4001;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5007;

public class LoggingExchangeCompletionListener implements ExchangeCompletionListener {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingExchangeCompletionListener.class);
    private final AuthRepResponse authRepResponse;
    public static final AttachmentKey<Long> APIM_HANDLE_START_TIME = AttachmentKey.create(Long.class);
    public static final AttachmentKey<Long> APIM_HANDLE_END_TIME = AttachmentKey.create(Long.class);
    public static final AttachmentKey<Long> PROXY_HANDLE_START_TIME = AttachmentKey.create(Long.class);

    public LoggingExchangeCompletionListener(AuthRepResponse authRepResponse) {
        this.authRepResponse = authRepResponse;
    }

    @Override
    public void exchangeEvent(HttpServerExchange e, NextListener next) {
        CorrelationHandler.setupMDC(e);
        int statusCode = e.getStatusCode();

        if (ErrorResponseHelper.isNotMonitoringUrl(e.getRequestPath())) {
            writeLog(e, statusCode, extractHandleDuration(e, APIM_HANDLE_START_TIME), extractHandleDuration(e, APIM_HANDLE_END_TIME), extractHandleDuration(e, PROXY_HANDLE_START_TIME));
        }

        MDC.clear();
        if (next != null) {
            next.proceed();
        }
    }

    private long extractHandleDuration(HttpServerExchange e, AttachmentKey<Long> key) {
        Long attachment = e.getAttachment(key);
        if (attachment == null) {
            return -1;
        } else {
            return attachment;
        }
    }

    private void writeLog(HttpServerExchange e, int statusCode, long apimStartTime, long apimEndTime, long proxyStartTime) {
        if (statusCode >= 200 && statusCode < 500) {
            if (LOG.isInfoEnabled()) {
                LOG.info(APIM_4001.pattern(), logArguments(e, authRepResponse, apimStartTime, apimEndTime, proxyStartTime));
            }
        } else {
            if (LOG.isWarnEnabled()) {
                LOG.warn(APIM_5007.pattern(), logArguments(e, authRepResponse, apimStartTime, apimEndTime, proxyStartTime));
            }
        }
    }

    private Object[] logArguments(HttpServerExchange e, AuthRepResponse authRepResponse, long apimStartTime, long apimEndTime, long proxyStartTime) {
        final long currentTimeMillis = System.currentTimeMillis();
        final ProxyExchangeStatus attachment = e.getAttachment(LoggingProxyCallback.PROXY_REQUEST_STATUS);
        String message = "";
        if (authRepResponse.isAllowed()) {
            if (ProxyExchangeStatus.COMPLETED.equals(attachment)) {
                message = "Connection to backend completed";
            } else {
                message = "Connection to backend failed";
            }
        } else {
            message = "Client not allowed";
        }
        return new Object[]{
                message,
                e.getStatusCode(),
                currentTimeMillis - apimStartTime,
                apimEndTime - apimStartTime,
                proxyStartTime > 0 ? currentTimeMillis - proxyStartTime : "UNKNOWN",
                attachment != null ? attachment : "UNKNOWN",
                authRepResponse.getClientId(),
                authRepResponse.isAllowed(),
                e.getRequestMethod(),
                e.getRequestPath(),
                e.getQueryString()};
    }
}
