package ch.sbb.integration.api.adapter.service.apiwatch;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.cache.TokenToParsedTokenCache;
import ch.sbb.integration.api.adapter.service.configuration.OperationMode;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.utils.AuthUtils;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;

public class ApiWatch {
    private static final Logger LOG = LoggerFactory.getLogger(ApimAdapterService.class);

    public static final int HTTP_STATUS_OK = 200;
    public static final String RESPONSE_CONTENT_TYPE = "application/json";
    public static final String QUERY_PARAM_NAME_TARGET_URL = "targetUrl";

    private static final String API_WATCH_CLIENT_ID = "api-watch-client";
    private static final int TIMEOUT_IN_MILLIS = 5_000;

    private ApimAdapterConfig adapterConfig;
    private OfflineConfigurationCacheRepo offlineConfigurationCacheRepo;
    private OperationMode operationMode;

    private final TokenToParsedTokenCache tokenToParsedTokenCache;
    private final ResteasyClient rest;

    public ApiWatch(ApimAdapterConfig adapterConfig, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo, TokenToParsedTokenCache tokenToParsedTokenCache, OperationMode operationMode) {
        this.adapterConfig = adapterConfig;
        this.offlineConfigurationCacheRepo = offlineConfigurationCacheRepo;
        this.tokenToParsedTokenCache = tokenToParsedTokenCache;
        this.rest = new RestConfig(TIMEOUT_IN_MILLIS, TIMEOUT_IN_MILLIS).newRestEasyClient();
        this.operationMode = operationMode;
    }

    public boolean isApiWatchRequest(String token, HttpMethod httpMethod) {
        return isApiWatchRequest(token, httpMethod != null ? httpMethod.name() : null);
    }

    public boolean isApiWatchRequest(String token, String httpMethod) {
        return isApiWatchRequest(tokenToParsedTokenCache.get(token), httpMethod);
    }

    public boolean isApiWatchRequest(OAuthToken token, String httpMethod) {
        boolean apiWatchRequest = token != null && token.isValid() && API_WATCH_CLIENT_ID.equals(token.getClientId()) && "GET".equalsIgnoreCase(httpMethod);
        LOG.debug("ApiWatchRequest={}", apiWatchRequest);
        return apiWatchRequest;
    }

    public void writeResponse(HttpServletResponse httpResponse) throws IOException {
        httpResponse.setStatus(HTTP_STATUS_OK);
        final PrintWriter writer = httpResponse.getWriter();
        writer.println(buildResponse());
        writer.flush();
    }

    public String buildResponse() throws JsonProcessingException {
        return buildResponse(null);
    }

    public String buildResponse(String responseContent) throws JsonProcessingException {
        final ApiWatchResponse response = new ApiWatchResponse();
        response.setCacheLocation(adapterConfig.getCacheLocation());
        response.setOfflineConfigurationCache(offlineConfigurationCacheRepo.getState().toString());
        response.setOperationMode(operationMode != null ? operationMode.toString() : "n/a");
        response.setVersion(getClass().getPackage().getImplementationVersion());
        response.setBackendResponse(responseContent != null ? responseContent : "no backend called");
        return new ObjectMapper().writeValueAsString(response);
    }

    public String pingBackend(String token, String targetUrl) {
        try (Response response = rest.target(targetUrl)
                .request()
                .header(AuthUtils.HTTP_AUTHORIZATION_HEADER_NAME, AuthUtils.mapJwtToHttpAuthorizationHeaderValue(token))
                .get()) {
            return response.readEntity(String.class);
        }
    }
}
