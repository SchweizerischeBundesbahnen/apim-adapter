package ch.sbb.integration.api.adapter.service.utils;

import java.util.regex.Pattern;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_3016;

public class AuthUtils {
    public static final String RH_SSO_REALM = "SBB_Public";
    public static final String HTTP_WWW_AUTHENTICATE_HEADER_NAME = "WWW-Authenticate";

    public static final String HTTP_AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String HTTP_AUTHORIZATION_HEADER_VALUE_BEARER_PREFIX = "Bearer ";
    private static final Pattern BEARER_PATTERN = Pattern.compile(HTTP_AUTHORIZATION_HEADER_VALUE_BEARER_PREFIX);

    public static String extractJwtFromAuthHeader(String httpAuthorizationHeaderValue) {
        if (httpAuthorizationHeaderValue != null) {
            return BEARER_PATTERN.matcher(httpAuthorizationHeaderValue).replaceFirst("").trim();
        } else {
            return httpAuthorizationHeaderValue;
        }
    }

    public static String mapJwtToHttpAuthorizationHeaderValue(String token) {
        if (token == null) {
            throw new IllegalArgumentException(APIM_3016.pattern());
        }
        return HTTP_AUTHORIZATION_HEADER_VALUE_BEARER_PREFIX + token;
    }
}
