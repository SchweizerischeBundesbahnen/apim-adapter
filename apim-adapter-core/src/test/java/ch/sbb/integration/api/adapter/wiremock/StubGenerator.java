package ch.sbb.integration.api.adapter.wiremock;

import ch.sbb.integration.api.adapter.util.TokenIssuerKeys;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ch.sbb.integration.api.adapter.util.Utilities.readFile;
import static ch.sbb.integration.api.adapter.wiremock.UrlMappingRules.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.http.HttpStatus.*;

/**
 * Created by u217269 on 10.04.2018.
 */
public class StubGenerator {

    public static void instantiateAll() {
        threescaleAdminApplicationStub();
        instantiateStubForAuthorizeAndSync();
        instantiateStubForReports();
        instantiateStubMappingRules();
        instantiateStubForMetric();
        instantiateStubForSbbPublicTokenIssuer();
        instantiateStubForSbbPublicTokenIssuerJwks();
        instantiateStubFor403();
        instantiateStubFor500();
    }

    public static StubMapping instantiateStubForAuthorizeAndSync() {
        return stubFor(get(patternForAuthorization())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(StubGenerator.authResponseForCurrentPeriod(60, 3))));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWithCustomAttributes(int periodLengthInSecounds, int usage) {
        return stubFor(get(patternForAuthorization())
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(StubGenerator.authResponseForCurrentPeriod(periodLengthInSecounds, usage))));
    }


    public static StubMapping instantiateStubForAuthorizeAndSyncWithAdditionalClient(int periodLengthInSeconds, int currentUsage) {
        return stubFor(get(patternForAuthorization())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(StubGenerator.authResponseForCurrentPeriodWithAdditionalClientStat(periodLengthInSeconds, currentUsage))));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWithNewPlan() {
        return stubFor(get(patternForAuthorization())
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(StubGenerator.newPlanAuthResponseForCurrentPeriod(60, 5))));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncAfterApplicationSuspention() {
        return stubFor(get(patternForAuthorization())
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(SC_CONFLICT)
                        .withBody(StubGenerator.authResponseOfSuspendedSubscription(60, 3))));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWithInvalidClientId() {
        return stubFor(get(patternForAuthorization())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_NOT_FOUND)
                        .withBody(StubGenerator.authResponseForAnInvalidClientId())));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWith3ScaleError() {
        return stubFor(get(patternForAuthorization())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_SERVICE_UNAVAILABLE)));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWithEternity(int periodLengthInSeconds, int currentUsage) {
        return stubFor(get(patternForAuthorization())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(StubGenerator.newPlanWithEternityAndPeriod(periodLengthInSeconds, currentUsage))));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWithExceededLimit() {
        return stubFor(get(patternForAuthorization())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_CONFLICT)
                        .withBody(StubGenerator.planWithExceededLimit(60, 0))));
    }

    public static StubMapping instantiateStubForAuthorizeAndSyncWithClientNotFound() {
        return stubFor(get(patternForAuthorization())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_NOT_FOUND)
                        .withBody(StubGenerator.errorApplicationNotFound("123456"))));
    }

    public static void instantiateStubForProxyRules() {
        stubFor(get(patternForProxyRules())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(readFile("/stubs/stub.proxyrules.json"))));
    }

    public static void instantiateStubMappingRules() {
        stubFor(get(patternForMappingRules())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(readFile("/stubs/stub.mappingrules.json"))));
    }

    public static void instantiateStubMappingRulesWithAddtitionalMetric() {
        stubFor(get(patternForMappingRules())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withBody(readFile("/stubs/stub.mappingrules.additional.metric.json"))));
    }


    public static void instantiateStubForReports() {
        stubFor(post(patternForReporting())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(SC_ACCEPTED)));
    }

    public static void instantiateStubFor403() {
        stubFor(get(patternFor400())
                .atPriority(100)
                .willReturn(aResponse()
                        .withStatus(SC_FORBIDDEN)));
    }

    public static void instantiateStubFor500() {
        stubFor(get(patternFor500())
                .atPriority(100)
                .willReturn(aResponse()
                        .withStatus(SC_INTERNAL_SERVER_ERROR)));
    }

    public static void instantiateStubWith20SecondsDelay() {

        stubFor(get(patternForDelay())
                .atPriority(100)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withFixedDelay(20_000)));
    }

    public static void instantiateStubForSbbPublicTokenIssuer() {
        stubFor(get(patternForOpenIdConnectConfig("/auth/realms/SBB_Public/"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withBody("{\n" +
                                "   \"issuer\":\"http://localhost:8099/auth/realms/SBB_Public\",\n" +
                                "   \"authorization_endpoint\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/auth\",\n" +
                                "   \"token_endpoint\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/token\",\n" +
                                "   \"token_introspection_endpoint\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/token/introspect\",\n" +
                                "   \"userinfo_endpoint\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/userinfo\",\n" +
                                "   \"end_session_endpoint\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/logout\",\n" +
                                "   \"jwks_uri\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/certs\",\n" +
                                "   \"introspection_endpoint\":\"http://localhost:8099/auth/realms/SBB_Public/protocol/openid-connect/token/introspect\"\n" +
                                "}")
                        .withStatus(SC_OK)));
    }

    public static void instantiateStubForSbbPublicTokenIssuerWithError() {
        stubFor(get(patternForOpenIdConnectConfig("/auth/realms/SBB_Public/"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withBody("error")
                        .withStatus(SC_INTERNAL_SERVER_ERROR)));
    }

    public static StubMapping instantiateStubForSbbPublicTokenIssuerJwks() {
        final List<TokenIssuerKeys.Key> keys = TokenIssuerKeys.getKeys();
        final String response = String.format("{\"keys\":[\n%s,\n%s]}", toJwksEntry(keys.get(0)), toJwksEntry(keys.get(1)));
        return stubFor(get(urlMatching("/auth/realms/SBB_Public/protocol/openid-connect/certs"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withBody(response)
                        .withStatus(SC_OK)));
    }


    public static StubMapping instantiateStubForSbbPublicTokenIssuerJwksWithNewKey() {
        final List<TokenIssuerKeys.Key> keys = TokenIssuerKeys.getKeys();
        final String response = String.format("{\"keys\":[\n%s,\n%s,\n%s]}", toJwksEntry(keys.get(0)), toJwksEntry(keys.get(1)), toJwksEntry(keys.get(2)));
        return stubFor(get(urlMatching("/auth/realms/SBB_Public/protocol/openid-connect/certs"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withBody(response)
                        .withStatus(SC_OK)));
    }

    public static StubMapping instantiateStubForSbbPublicTokenIssuerJwksWithError() {
        return stubFor(get(urlMatching("/auth/realms/SBB_Public/protocol/openid-connect/certs"))
                .atPriority(5)
                .willReturn(aResponse()
                        .withBody("error")
                        .withStatus(SC_INTERNAL_SERVER_ERROR)));
    }

    private static String toJwksEntry(TokenIssuerKeys.Key key) {
        return String.format("{\n" +
                "   \"kid\":\"%s\",\n" +
                "   \"kty\":\"RSA\",\n" +
                "   \"alg\":\"RS256\",\n" +
                "   \"use\":\"sig\",\n" +
                "   \"n\":\"%s\",\n" +
                "   \"e\":\"%s\"\n" +
                "}\n", key.getKeyId(), key.getPublicKeyModulusBase64(), key.getPublicKeyExponentBase64());
    }

    public static void errorStub() {
        stubFor(get(errorMatcher())
                .atPriority(1000)
                .willReturn(aResponse()
                        .withBody("error")
                        .withStatus(SC_INTERNAL_SERVER_ERROR)));
    }

    public static void emptyStub() {
        stubFor(get(errorMatcher())
                .atPriority(1000)
                .willReturn(aResponse()
                        .withBody("")
                        .withStatus(SC_OK)));
    }

    public static StubMapping threescaleAdminApplicationStub() {
        return stubFor(get(patternForApplicationXml())
                .atPriority(10)
                .willReturn(aResponse()
                        .withBody("not empty")
                        .withStatus(SC_OK)));
    }


    public static void instantiateStubForMetric() {
        stubFor(get(patternForMetric())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readFile("/stubs/stub.metric.json"))));
    }

    public static void instantiateStubForMetricsWithDelay() {
        stubFor(post(patternForReporting())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(SC_ACCEPTED)
                        .withFixedDelay(5_000)));
    }

    // Comes together with an Http 200
    public static String authResponseForCurrentPeriod(int periodLengthInSeconds, int currentUsage) {
        String periodStart = Instant.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        String periodEnd = Instant.now().plusSeconds(periodLengthInSeconds).atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "    <authorized>true</authorized>\n" +
                "    <application>\n" +
                "        <id>084e8c30</id>\n" +
                "        <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "        <redirect_url></redirect_url>\n" +
                "    </application>\n" +
                "    <plan>Limited</plan>\n" +
                "    <usage_reports>\n" +
                "        <usage_report metric=\"hits\" period=\"hour\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>5</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations-noworries\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>0</max_value>\n" +
                "            <current_value>0</current_value>\n" +
                "        </usage_report>\n" +
                "		 <usage_report metric=\"double-wildcard-pattern\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "		 <usage_report metric=\"single-wildcard-pattern\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "		 <usage_report metric=\"single-wildcard-dollar-pattern\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "    </usage_reports>\n" +
                "</status>";
    }

    // Comes together with an Http 200
    public static String authResponseForCurrentPeriodWithAdditionalClientStat(int periodLengthInSeconds, int currentUsage) {
        String periodStart = Instant.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        String periodEnd = Instant.now().plusSeconds(periodLengthInSeconds).atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "    <authorized>true</authorized>\n" +
                "    <application>\n" +
                "        <id>084e8c30</id>\n" +
                "        <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "        <redirect_url></redirect_url>\n" +
                "    </application>\n" +
                "    <plan>Limited</plan>\n" +
                "    <usage_reports>\n" +
                "        <usage_report metric=\"hits\" period=\"hour\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>5</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations-noworries\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>0</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations-new\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>20</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "    </usage_reports>\n" +
                "</status>";
    }

    // Comes together with an Http 409
    public static String authResponseOfSuspendedSubscription(int periodLengthInSeconds, int currentUsage) {
        String periodStart = Instant.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        String periodEnd = Instant.now().plusSeconds(periodLengthInSeconds).atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "    <authorized>false</authorized>\n" +
                "    <reason>application is not active</reason>\n" +
                "    <application>\n" +
                "        <id>084e8c30</id>\n" +
                "        <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "        <redirect_url></redirect_url>\n" +
                "    </application>\n" +
                "    <plan>Limited</plan>\n" +
                "    <usage_reports>\n" +
                "        <usage_report metric=\"hits\" period=\"hour\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>5</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations-noworries\" period=\"minute\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>0</max_value>\n" +
                "            <current_value>0</current_value>\n" +
                "        </usage_report>\n" +
                "    </usage_reports>\n" +
                "</status>";
    }

    // Comes together with an Http 200
    public static String newPlanAuthResponseForCurrentPeriod(int periodLengthInSeconds, int currentUsage) {
        String periodStart = Instant.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        String periodEnd = Instant.now().plusSeconds(periodLengthInSeconds).atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "    <authorized>true</authorized>\n" +
                "    <application>\n" +
                "        <id>084e8c30</id>\n" +
                "        <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "        <redirect_url></redirect_url>\n" +
                "    </application>\n" +
                "    <plan>Updated</plan>\n" +
                "    <usage_reports>\n" +
                "        <usage_report metric=\"hits\" period=\"hour\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "    </usage_reports>\n" +
                "</status>";
    }


    // Comes together with an Http 200
    public static String newPlanWithEternityAndPeriod(int periodLengthInSeconds, int currentUsage) {
        String periodStart = Instant.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        String periodEnd = Instant.now().plusSeconds(periodLengthInSeconds).atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "    <authorized>true</authorized>\n" +
                "    <application>\n" +
                "        <id>084e8c30</id>\n" +
                "        <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "        <redirect_url></redirect_url>\n" +
                "    </application>\n" +
                "    <plan>Updated</plan>\n" +
                "    <usage_reports>\n" +
                "        <usage_report metric=\"hits\" period=\"hour\">\n" +
                "            <period_start>" + periodStart + "</period_start>\n" +
                "            <period_end>" + periodEnd + "</period_end>\n" +
                "            <max_value>1000</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>\n" +
                "        <usage_report metric=\"locations\" period=\"eternity\">\n" +
                "            <max_value>5</max_value>\n" +
                "            <current_value>" + currentUsage + "</current_value>\n" +
                "        </usage_report>" +
                "    </usage_reports>\n" +
                "</status>";
    }

    // Comes together with an Http 409
    public static String planWithExceededLimit(int periodLengthInSeconds, int currentUsage) {
        String periodStart = Instant.now().atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        String periodEnd = Instant.now().plusSeconds(periodLengthInSeconds).atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "  <authorized>false</authorized>\n" +
                "  <reason>usage limits are exceeded</reason>\n" +
                "  <application>\n" +
                "    <id>084e8c30</id>\n" +
                "    <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "    <redirect_url></redirect_url>\n" +
                "  </application>\n" +
                "  <plan>PartialExceededPlan</plan>\n" +
                "  <usage_reports>\n" +
                "    <usage_report metric=\"locations\" period=\"eternity\" exceeded=\"true\">\n" +
                "      <max_value>5</max_value>\n" +
                "      <current_value>8</current_value>\n" +
                "    </usage_report>\n" +
                "    <usage_report metric=\"hits\" period=\"day\">\n" +
                "       <period_start>" + periodStart + "</period_start>\n" +
                "       <period_end>" + periodEnd + "</period_end>\n" +
                "       <max_value>1000</max_value>\n" +
                "       <current_value>" + currentUsage + "</current_value>\n" +
                "    </usage_report>\n" +
                "  </usage_reports>\n" +
                "</status>";
    }

    public static String errorApplicationNotFound(String clientId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<error code=\"application_not_found\">application with id=\"" + clientId + "\" was not found</error>";
    }


    // Comes together with an Http 200
    public static String authResponseForAnUnlimitedPlan() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<status>\n" +
                "  <authorized>true</authorized>\n" +
                "  <application>\n" +
                "    <id>084e8c30</id>\n" +
                "    <key>6887db2af03e42962a0a688da0986deb</key>\n" +
                "    <redirect_url></redirect_url>\n" +
                "  </application>\n" +
                "  <plan>Unlimited</plan>\n" +
                "</status>";
    }

    // Comes together with an Http 404
    public static String authResponseForAnInvalidClientId() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<error code=\"application_not_found\">application with id=\"084e8c303\" was not found</error>";
    }

    public static void removeStub(StubMapping stubMapping) {
        WireMock.removeStub(stubMapping);
    }

}
