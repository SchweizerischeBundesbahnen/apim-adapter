package ch.sbb.integration.api.adapter.model.oidc;

public class OidcConfigUtils {
    private OidcConfigUtils() {
    }

    public static String issuerToOpenIdConfigUrl(String issuerUrl) {
        return issuerUrl + (issuerUrl.endsWith("/") ? "" : "/") + ".well-known/openid-configuration";
    }
}
