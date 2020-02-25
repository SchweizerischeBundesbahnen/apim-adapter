package ch.sbb.integration.api.gateway.proxy;

import ch.sbb.integration.api.gateway.handler.CorrelationHandler;
import ch.sbb.integration.api.gateway.handler.LoggingExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_4006;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5011;

public class LoggingProxyCallback implements ProxyCallback<ProxyConnection> {
    // requested by APIM gateway users in order to access the status in gateway extensions (custom handler)
    public static final AttachmentKey<ProxyExchangeStatus> PROXY_REQUEST_STATUS = AttachmentKey.create(ProxyExchangeStatus.class);
    private static final Logger LOG = LoggerFactory.getLogger(LoggingProxyCallback.class);

    private final ProxyCallback<ProxyConnection> delegate;

    LoggingProxyCallback(ProxyCallback<ProxyConnection> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void completed(HttpServerExchange exchange, ProxyConnection result) {
        CorrelationHandler.setupMDC(exchange);
        if (LOG.isDebugEnabled()) {
            LOG.debug(APIM_4006.pattern(), logArguments("connection to backend completed", exchange));
        }
        exchange.putAttachment(PROXY_REQUEST_STATUS, ProxyExchangeStatus.COMPLETED);
        MDC.clear();
        delegate.completed(exchange, result);
        ProxyStatistics.get().incrementCompleted();
    }

    @Override
    public void failed(HttpServerExchange exchange) {
        CorrelationHandler.setupMDC(exchange);
        LOG.warn(APIM_5011.pattern(), logArguments("failed to establish a connection to the backend", exchange));
        exchange.putAttachment(PROXY_REQUEST_STATUS, ProxyExchangeStatus.FAILED);
        MDC.clear();
        delegate.failed(exchange);
        ProxyStatistics.get().incrementFailed();
    }

    @Override
    public void couldNotResolveBackend(HttpServerExchange exchange) {
        CorrelationHandler.setupMDC(exchange);
        LOG.warn(APIM_5011.pattern(), logArguments("couldNotResolveBackend", exchange));
        exchange.putAttachment(PROXY_REQUEST_STATUS, ProxyExchangeStatus.COULD_NOT_RESOLVE_BACKEND);
        MDC.clear();
        delegate.couldNotResolveBackend(exchange);
        ProxyStatistics.get().incrementUnresolvableBackend();
    }

    @Override
    public void queuedRequestFailed(HttpServerExchange exchange) {
        CorrelationHandler.setupMDC(exchange);
        LOG.warn(APIM_5011.pattern(), logArguments("queuedRequestFailed", exchange));
        exchange.putAttachment(PROXY_REQUEST_STATUS, ProxyExchangeStatus.QUEUED_REQUEST_FAILED);
        MDC.clear();
        delegate.queuedRequestFailed(exchange);
        ProxyStatistics.get().incrementQueuedRequestFailed();
    }


    private Object[] logArguments(String message, HttpServerExchange e) {
        return new Object[]{
                message,
                System.currentTimeMillis() - e.getAttachment(LoggingExchangeCompletionListener.PROXY_HANDLE_START_TIME),
                e.getRequestMethod(),
                e.getRequestPath(),
                e.getQueryString()};
    }
}
