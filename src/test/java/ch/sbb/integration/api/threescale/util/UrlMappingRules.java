package ch.sbb.integration.api.threescale.util;

import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Created by u217269 on 26.02.2018.
 */
public class UrlMappingRules {

    public static UrlPattern patternForMappingRules() {
        return urlMatching("/admin/api/services/.*/proxy/mapping_rules.json.*");
    }

    public static UrlPattern patternForAuthorization() {
        return urlMatching("/transactions/oauth_authorize.xml.*");
    }

    public static UrlPattern patternForReporting() {
        return urlMatching("/transactions.xml");
    }

    public static UrlPattern patternForMetrics(String metricId) {
        return urlMatching("/admin/api/services/" + ThreeScaleConfig.serviceId() + "/metrics/" + metricId + ".json.*");
    }

}
