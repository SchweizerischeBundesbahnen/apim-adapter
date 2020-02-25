package ch.sbb.integration.api.adapter.service.utils;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuer;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1033;

public class ErrorResponseHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorResponseHelper.class);

    private ApimAdapterConfig config;

    public ErrorResponseHelper(ApimAdapterConfig config) {
        this.config = config;
    }

    public AuthRepResponse createErrorAuthResponse(OAuthToken oAuthToken, ErrorReason reason, String path, String queryString, HttpMethod method, List<String> metricSysNames) {
        final String clientId;
        final String realm;
        if (oAuthToken != null) {
            clientId = oAuthToken.getClientId();
            final TokenIssuer tokenIssuer = oAuthToken.getTokenIssuer();
            realm = tokenIssuer != null ? tokenIssuer.getRealm() : "";
        } else {
            clientId = null;
            realm = "";

        }
        return createErrorAuthResponse(realm, clientId, reason, path, queryString, method, metricSysNames);
    }

    public AuthRepResponse createErrorAuthResponse(String realm, String clientId, ErrorReason reason, String path, String queryString, HttpMethod method, List<String> metricSysNames) {
        if (ErrorResponseHelper.isNotMonitoringUrl(path)) {
            LOG.info(APIM_1033.pattern(), clientId, reason.getHttpStatus(), reason.getMessage(), path, method);
        }

        // I would vote for production mode removal
        if (config.isAdapterProductionMode()) {
            // not sure if we should distinguish the status codes here - this makes it harder to troubleshoot issues.
            return handleAuthTokenRelatedReasons(realm, clientId, reason, path, queryString, method, metricSysNames, reason.getMessage()).
                    orElse(new AuthRepResponse(false, clientId, HttpStatus.SC_FORBIDDEN, metricSysNames, "forbidden", path, queryString, method));
        } else {
            final String msg = createExtendedMessage(reason.getMessage(), path, queryString, method);
            return handleAuthTokenRelatedReasons(realm, clientId, reason, path, queryString, method, metricSysNames, msg).
                    orElse(new AuthRepResponse(false, clientId, reason.getHttpStatus(), metricSysNames, msg, path, queryString, method));
        }
    }

    private Optional<AuthRepResponse> handleAuthTokenRelatedReasons(String realm, String clientId, ErrorReason reason, String path, String queryString, HttpMethod method, List<String> metricSysNames, String msg) {
        if (realm == null || realm.isEmpty()) {
            realm = AuthUtils.RH_SSO_REALM;
        }

        switch (reason) {
            case TOKEN_VALIDATION_FAILED:
            case EXPIRED_OR_INVALID:
                final String wwwAuthenticateBearerInvalidToken = String.format("Bearer realm=\"%s\",\n" +
                        "error=\"invalid_token\",\n" +
                        "error_description=\"The access token expired\"\n", realm);
                return Optional.of(new AuthRepResponse(false, clientId, reason.getHttpStatus(), wwwAuthenticateBearerInvalidToken, metricSysNames, msg, path, queryString, method));
            case UNAUTHORIZED:
                final String wwwAuthenticateBearer = String.format("Bearer realm=\"%s\"", realm);
                return Optional.of(new AuthRepResponse(false, clientId, reason.getHttpStatus(), wwwAuthenticateBearer, metricSysNames, msg, path, queryString, method));
            default:
                return Optional.empty();
        }
    }

    static String createExtendedMessage(String message, String path, String queryString, HttpMethod method) {
        return message + ": Path: '" + path + "', " +
                "Query: '" + ((queryString == null) ? "" : queryString) + "', " +
                "Method: '" + method + "'";
    }

    public static boolean isNotMonitoringUrl(String path) {
        if (path == null) {
            return true;
        }

        if ("/expectingForbidden4HealthCheck".equals(path)) {
            return false;
        }

        if (path.endsWith("/apim/check")) {
            return false;
        }
        return true;
    }

}
