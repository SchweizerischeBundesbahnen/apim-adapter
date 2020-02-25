package ch.sbb.integration.api.adapter.wiremock;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

import static ch.sbb.integration.api.adapter.model.oidc.OidcConfigUtils.issuerToOpenIdConfigUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Created by u217269 on 26.02.2018.
 */
public class UrlMappingRules {

	private static ApimAdapterConfig apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
	
    public static UrlPattern patternForMappingRules() {
        return urlMatching("/admin/api/services/.*/proxy/mapping_rules.json.*");
    }

    public static UrlPattern patternForAuthorization() {
        return urlMatching("/transactions/oauth_authorize.xml.*");
    }

    public static UrlPattern patternForReporting() {
        return urlMatching("/transactions.xml");
    }

    public static UrlPattern patternFor400() {
        return urlMatching("/YouShallNotPass");
    }

    public static UrlPattern patternFor500() {
        return urlMatching("/misterStarkIDontFeelSoGood");
    }

    public static UrlPattern patternForDelay() {
        return urlMatching("/iWillWaitHere");
    }

    public static UrlPattern patternForMetric() {
        return urlMatching("/admin/api/services/" + apimAdapterConfig.getAdapterServiceId() + "/metrics.json.*");
    }

    public static UrlPattern errorMatcher() {
        return urlMatching("/.*");
    }

    public static UrlPattern patternForProxyRules() {
        return urlMatching("/admin/api/services/" + apimAdapterConfig.getAdapterServiceId() + "/proxy/configs/production/latest.json.*");
    }

    public static UrlPattern patternForApplicationXml() {
        return urlMatching("/admin/api/applications.xml.*");
    }

    public static UrlPattern patternForOpenIdConnectConfig(String issuerUrl) {
        return urlMatching(issuerToOpenIdConfigUrl(issuerUrl));
    }
    public static UrlPattern patternForPublicKey() {
        return urlMatching("/auth/realms/SBB_Public/");
    }


}
