package ch.sbb.integration.api.gateway.handler;

import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.gateway.ApimSingleton;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ReportHitsExchangeCompletionListener implements ExchangeCompletionListener {
    private static final Logger LOG = LoggerFactory.getLogger(ReportHitsExchangeCompletionListener.class);

    private AuthRepResponse authRepResponse;

    ReportHitsExchangeCompletionListener(AuthRepResponse authRepResponse) {
        this.authRepResponse = authRepResponse;
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener next) {
        CorrelationHandler.setupMDC(exchange);

        LOG.debug("invoked reportStatusCode for authRepResponse={}", authRepResponse);
        ApimSingleton.get().reportHit(authRepResponse, exchange.getStatusCode());

        MDC.clear();
        if (next != null) {
            next.proceed();
        }
    }
}
