package ch.sbb.integration.api.adapter.config;

import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RestClientTimeoutTest {
    private static final Logger LOG = LoggerFactory.getLogger(RestClientTimeoutTest.class);

    private static final int TIMEOUT_IN_MILLIS = 1_000;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8099);

    private ResteasyClient resteasyClient;

    @Before
    public void init() {
        RestConfig restConfig = new RestConfig(TIMEOUT_IN_MILLIS, TIMEOUT_IN_MILLIS);
        this.resteasyClient = restConfig.newRestEasyClient();

        wireMockRule.resetAll();
        stubFor(get(urlEqualTo("/longrunning"))
                .willReturn(aResponse().withFixedDelay(TIMEOUT_IN_MILLIS + 2000) // delay 2000ms longer than timeout
                        .withStatus(200)));
    }

    @Test
    public void testReadTimeout() {
        StopWatch sw = new StopWatch().start();
        try (Response ignored = resteasyClient.target("http://localhost:8099/longrunning")
                .request()
                .get()) {
            fail("Expected SocketTimeoutException");
        } catch (ProcessingException e) {
            SocketTimeoutException ste = getRootCause(e);
            assertEquals("Read timed out", ste.getMessage());
            assertElapsedTime(sw);
        }
    }


    @Test
    public void testConnectTimeout() {
        StopWatch sw = new StopWatch().start();
        // example domain reserved by rfc - port 81 does not response, so that is the perfect test case
        try (Response ignored = resteasyClient.target("http://example.com:81")
                .request()
                .get()) {
            fail("Expected SocketTimeoutException");
        } catch (ProcessingException e) {
            Throwable rootCause = getRootCause(e);
            assertTrue(rootCause instanceof SocketTimeoutException || rootCause instanceof ConnectException);
            if (rootCause instanceof SocketTimeoutException) {
                assertEquals("connect timed out", rootCause.getMessage());
            }
            assertElapsedTime(sw);
        }
    }

    private void assertElapsedTime(StopWatch sw) {
        long elapsedTime = sw.stop().getMillis();
        LOG.info("elapsed time {} ms - timeout was set to {} ms", elapsedTime, TIMEOUT_IN_MILLIS);
        assertTrue(elapsedTime < TIMEOUT_IN_MILLIS + 500);
    }

    private <T extends Throwable> T getRootCause(Throwable t) {
        if (t.getCause() != null) {
            return getRootCause(t.getCause());
        }
        return (T) t;
    }

}