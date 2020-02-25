package ch.sbb.integration.api.gateway.handler;

import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.apiwatch.ApiWatch;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.gateway.ApimSingleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.Deque;
import java.util.Optional;

class ApiWatchHandlerDelegate {
    private final ApimAdapterService apimAdapterService;

    ApiWatchHandlerDelegate() {
        this.apimAdapterService = ApimSingleton.get();
    }

    boolean isApiWatchRequest(String token, HttpMethod httpMethod) {
        return apimAdapterService.getApiWatch().isApiWatchRequest(token, httpMethod);
    }

    void handleApiWatchRequest(HttpServerExchange exchange, String token) throws JsonProcessingException {
        final ApiWatch apiWatch = apimAdapterService.getApiWatch();
        final String backendCallResponse = extractSingleQueryParam(exchange, ApiWatch.QUERY_PARAM_NAME_TARGET_URL)
                .map(s -> apiWatch.pingBackend(token, s))
                .orElse(null);

        final String response = apiWatch.buildResponse(backendCallResponse);
        exchange.setStatusCode(ApiWatch.HTTP_STATUS_OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ApiWatch.RESPONSE_CONTENT_TYPE);
        exchange.getResponseSender().send(response);
    }

    private Optional<String> extractSingleQueryParam(HttpServerExchange exchange, String queryParamName) {
        Deque<String> targetUrlDeque = exchange.getQueryParameters().get(queryParamName);
        if (targetUrlDeque != null && !targetUrlDeque.isEmpty()) {
            return Optional.ofNullable(targetUrlDeque.getFirst());
        } else {
            return Optional.empty();
        }
    }

}
