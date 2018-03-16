package ch.sbb.integration.api.threescale.service;

import ch.sbb.integration.api.threescale.config.ThreeScaleConfig;
import ch.sbb.integration.api.threescale.model.AuthRepResponse;
import ch.sbb.integration.api.threescale.model.ClientStats;
import ch.sbb.integration.api.threescale.model.MetricUsage;
import ch.sbb.integration.api.threescale.service.utils.HttpMethod;
import ch.sbb.integration.api.threescale.util.TokenGenerator;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ch.sbb.integration.api.threescale.util.UrlMappingRules.*;
import static ch.sbb.integration.api.threescale.util.Utilities.readFile;
import static ch.sbb.integration.api.threescale.util.Utilities.tryToSleep;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

/**
 * Created by u217269 on 23.02.2018.
 */
public class ThreeScaleAdapterServiceTest {

    protected final String clientId = "084e8c30";
    protected final Integer[] metricIds = {85, 360, 364};

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8099);

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-junit.yml");
    }

    @Before
    public void initStubs() {

        // Auhtorize and Sync
        stubFor(get(patternForAuthorization())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(authResponseForCurrentPeriod(60, 3))));

        // Mapping Rules
        stubFor(get(patternForMappingRules())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readFile("/stubs/stub.mappingrules.json"))));

        // Report
        stubFor(post(patternForReporting())
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_ACCEPTED)));

        // Metrics
        stubsForMetricsCalls(metricIds);

    }

    private void stubsForMetricsCalls(Integer... metricIds) {
        for (Integer metricId : metricIds) {
            stubFor(get(patternForMetrics(String.valueOf(metricId)))
                    .atPriority(10)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(readFile("/stubs/stub.metric." + metricId + ".json"))));
        }
    }

    @Test
    public void testStandardRequestWithUnlimitedPlan() {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);
        stubFor(get(patternForAuthorization())
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(authResponseForAnUnlimitedPlan())));

        String path = "/locations";
        HttpMethod method = HttpMethod.GET;
        AuthRepResponse authRepResponse = service.authRep(validToken, path, method);

        assertTrue(authRepResponse.isAllowed());
        assertEquals(authRepResponse.getClientId(), clientId);
        assertNotNull(authRepResponse.getMessage());
    }

    @Test
    public void testStandardRequestWithinLimits() {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);

        String path = "/locations";
        HttpMethod method = HttpMethod.GET;
        AuthRepResponse authRepResponse = service.authRep(validToken, path, method);

        assertTrue(authRepResponse.isAllowed());
        assertEquals(authRepResponse.getClientId(), clientId);
        assertNotNull(authRepResponse.getMessage());
    }

    @Test
    public void testVerifyOneCallPerMetricId() {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);
        String path = "/locations";
        HttpMethod method = HttpMethod.GET;
        service.authRep(validToken, path, method);

        // Verify that only one call has been made per metric (needs to be cached - short lived)
        verify(metricIds.length, getRequestedFor(patternForMetrics(".*")));
    }

    @Test
    public void testReportingTo3Scale_simple() throws UnsupportedEncodingException {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);
        String path = "/locations";
        HttpMethod method = HttpMethod.GET;

        for (int i = 0; i < 5; i++) {
            service.authRep(validToken, path, method);
        }

        waitForSyncToBeDone();
        verify(postRequestedFor(patternForReporting()));

        List<LoggedRequest> reportingRequests = findAll(postRequestedFor(patternForReporting()));
        String decodedBody = URLDecoder.decode(reportingRequests.get(0).getBodyAsString());
        assertTrue(decodedBody.contains("[usage][hits]=5")); // 5 hits for metric with name "hits"
        assertTrue(decodedBody.contains("[usage][locations]=5")); // 5 hits for metric with name "locations"
    }

    @Test
    public void testReportingTo3Scale_verifyUsageStats() {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);
        String path = "/locations";
        HttpMethod method = HttpMethod.GET;

        for (int i = 0; i < 5; i++) {
            service.authRep(validToken, path, method);
        }

        waitForSyncToBeDone();

        service.authRep(validToken, path, method);

        ClientStats stats = service.readCurrentStats(clientId);
        assertUsages(stats, "hits", 3, 6, 1);
        assertUsages(stats, "locations", 3, 6, 1);
        assertUsages(stats, "locations-noworries", 0, 0, 0);
    }

    @Test
    public void testReportingTo3Scale_rollbackOfClientUsageWhenBackendIsNotAvailable() {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);
        stubFor(post(patternForReporting())
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        String path = "/locations";
        HttpMethod method = HttpMethod.GET;

        for (int i = 0; i < 5; i++) {
            service.authRep(validToken, path, method);
        }

        waitForSyncToBeDone();

        service.authRep(validToken, path, method);

        ClientStats stats = service.readCurrentStats(clientId);
        assertUsages(stats, "hits", 3, 6, 6);
        assertUsages(stats, "locations", 3, 6, 6);
        assertUsages(stats, "locations-noworries", 0, 0, 0);
    }

    @Test
    public void testStandardRequestExceedingLimits() {
        ThreeScaleAdapterService service = new ThreeScaleAdapterService();
        String validToken = TokenGenerator.generateValidToken(clientId, 30_000);
        String path = "/locations";
        HttpMethod method = HttpMethod.GET;

        AuthRepResponse authRepResponse = service.authRep(validToken, path, method);
        assertTrue(authRepResponse.isAllowed());
        String okMessage = authRepResponse.getMessage();

        for (int i = 0; i < 5; i++) {
            authRepResponse = service.authRep(validToken, path, method);
        }
        assertFalse(authRepResponse.isAllowed());
        assertNotNull(authRepResponse.getMessage());
        assertNotEquals(okMessage, authRepResponse.getMessage());
    }

    private void assertUsages(ClientStats stats, String metricName, int base, int countInPeriod, int countSinceLastSync) {
        MetricUsage hitsUsage = stats.getUsage(metricName);
        assertEquals(metricName + "|Base", base, hitsUsage.getBase().get());
        assertEquals(metricName + "|CountInPeriod", countInPeriod, hitsUsage.getCountInCurrentPeriod().get());
        assertEquals(metricName + "|CountSinceLastSync", countSinceLastSync, hitsUsage.getCountSinceLastSync().get());
    }

    private static String authResponseForCurrentPeriod(int periodLengthInSeconds, int currentUsage) {
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
                "    </usage_reports>\n" +
                "</status>";
    }

    private static String authResponseForAnUnlimitedPlan() {
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

    private void waitForSyncToBeDone() {
        tryToSleep((ThreeScaleConfig.syncRateInSeconds() + 1) * 1_000);
    }

}
