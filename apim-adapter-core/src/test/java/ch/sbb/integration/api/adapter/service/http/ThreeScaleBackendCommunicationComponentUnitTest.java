package ch.sbb.integration.api.adapter.service.http;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.model.reporting.ResponseSummary;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.serverError;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ThreeScaleBackendCommunicationComponentUnitTest {

    private static final Response OK_RESPONSE = noContent().build();
    private static final String METRIC = "METRIC";
    private static final String CLIENT_ID = "CLIENT_1";
    private static final String SERVICE_ID = "SERVICE_ID";
    private static final String BACKEND_TOKEN = "BACKEND_TOKEN";
    private ThreeScaleBackendCommunicationComponent testee;

    @Mock
    private ApimAdapterConfig mockedConfig;
    @Mock
    private RestConfig mockedRestConfig;
    @Mock
    private ResteasyClient mockedRestClient;
    @Mock
    private ResteasyWebTarget mockedWebTarget;
    @Mock
    private Invocation.Builder mockedBuilder;

    @Captor
    private ArgumentCaptor<Entity> captor;

    @Before
    public void setUp() throws Exception {
        createTestee(true);
    }

    private void createTestee(boolean reportResponseCode) {
        when(mockedConfig.getBackendUrl()).thenReturn("BACKEND_URL");
        when(mockedConfig.getBackendToken()).thenReturn(BACKEND_TOKEN);
        when(mockedConfig.getAdapterServiceId()).thenReturn(SERVICE_ID);
        when(mockedConfig.isReportResponseCode()).thenReturn(reportResponseCode);

        when(mockedRestConfig.newRestEasyClient()).thenReturn(mockedRestClient);
        when(mockedRestClient.target(eq("BACKEND_URL/transactions.xml"))).thenReturn(mockedWebTarget);
        when(mockedWebTarget.request()).thenReturn(mockedBuilder);

        testee = new ThreeScaleBackendCommunicationComponent(mockedConfig, mockedRestConfig);
    }

    @Test
    public void reportEmptyMap() {
        // Act
        boolean report = testee.report(new HashMap<>());

        // Assert
        assertThat(report, is(true));
        verifyZeroInteractions(mockedRestClient);
    }

    @Test
    public void reportSingleTransaction() {
        // Arrange
        Map<ResponseSummary, Long> hits = new HashMap<>();
        hits.put(new ResponseSummary(CLIENT_ID, 200, METRIC), 1L);
        when(mockedBuilder.post(any(Entity.class))).thenReturn(OK_RESPONSE);

        // Act
        boolean report = testee.report(hits);

        // Assert
        assertThat(report, is(true));
        verify(mockedBuilder).post(captor.capture());
        System.out.println(captor.getValue());
        Form f = ((Form)captor.getValue().getEntity());

        assertThat(f.asMap().size(), is(6)); //service_token + service_id + 4 per transaction
        assertThat(f.asMap().get("service_token"), hasItem(BACKEND_TOKEN));
        assertThat(f.asMap().get("service_id"), hasItem(SERVICE_ID));
        assertThat(f.asMap().get("transactions[0][app_id]"), hasItem(CLIENT_ID));
        assertThat(f.asMap().get("transactions[0][log][code]"), hasItem("200"));
        assertThat(f.asMap().get("transactions[0][usage][" + METRIC + "]"), hasItem("1"));
    }

    @Test
    public void reportErrorTransaction() {
        // Arrange
        Map<ResponseSummary, Long> hits = new HashMap<>();
        hits.put(new ResponseSummary(CLIENT_ID, 200, METRIC), 1L);
        when(mockedBuilder.post(any(Entity.class))).thenReturn(serverError().build());

        // Act
        boolean report = testee.report(hits);

        // Assert
        assertThat(report, is(false));
    }

    @Test
    public void reportExceptionTransaction() {
        // Arrange
        Map<ResponseSummary, Long> hits = new HashMap<>();
        hits.put(new ResponseSummary(CLIENT_ID, 200, METRIC), 1L);
        when(mockedBuilder.post(any(Entity.class))).thenThrow(new RuntimeException("error"));

        // Act
        boolean report = testee.report(hits);

        // Assert
        assertThat(report, is(false));
    }

    @Test
    public void report2TransactionResponseCode() {
        // Arrange
        Map<ResponseSummary, Long> hits = new HashMap<>();
        hits.put(new ResponseSummary(CLIENT_ID, 200, METRIC), 2L);
        when(mockedBuilder.post(any(Entity.class))).thenReturn(OK_RESPONSE);

        // Act
        boolean report = testee.report(hits);

        // Assert
        assertThat(report, is(true));
        verify(mockedBuilder).post(captor.capture());
        Form f = ((Form)captor.getValue().getEntity());

        assertThat(f.asMap().size(), is(10)); //service_token + service_id + 4 per transaction
        assertThat(f.asMap().get("transactions[0][app_id]"), hasItem(CLIENT_ID));
        assertThat(f.asMap().get("transactions[0][log][code]"), hasItem("200"));
        assertThat(f.asMap().get("transactions[0][usage][" + METRIC + "]"), hasItem("1"));
        assertThat(f.asMap().get("transactions[1][app_id]"), hasItem(CLIENT_ID));
        assertThat(f.asMap().get("transactions[1][log][code]"), hasItem("200"));
        assertThat(f.asMap().get("transactions[1][usage][" + METRIC + "]"), hasItem("1"));
    }

    @Test
    public void report1001TransactionResponseCode() {
        // Arrange
        Map<ResponseSummary, Long> hits = new HashMap<>();
        hits.put(new ResponseSummary(CLIENT_ID, 200, METRIC), 500L);
        hits.put(new ResponseSummary(CLIENT_ID, 201, METRIC), 501L);
        when(mockedBuilder.post(any(Entity.class))).thenReturn(OK_RESPONSE);

        // Act
        boolean report = testee.report(hits);

        // Assert
        assertThat(report, is(true));
        verify(mockedBuilder, times(2)).post(captor.capture());
        Form f = ((Form)captor.getAllValues().get(0).getEntity());

        assertThat(f.asMap().size(), is(4002)); //service_token + service_id + 4 per transaction
    }


    @Test
    public void report2TransactionWithNoResponseCode() {
        // Arrange
        createTestee(false);

        Map<ResponseSummary, Long> hits = new HashMap<>();
        hits.put(new ResponseSummary(CLIENT_ID, 200, METRIC), 2L);
        when(mockedBuilder.post(any(Entity.class))).thenReturn(OK_RESPONSE);

        // Act
        boolean report = testee.report(hits);

        // Assert
        assertThat(report, is(true));
        verify(mockedBuilder).post(captor.capture());
        Form f = ((Form)captor.getValue().getEntity());

        assertThat(f.asMap().size(), is(5)); //service_token + service_id + 3 per transaction
        assertThat(f.asMap().get("transactions[0][app_id]"), hasItem(CLIENT_ID));
        assertThat(f.asMap().containsKey("transactions[0][log][code]"), is(false));
        assertThat(f.asMap().get("transactions[0][usage][" + METRIC + "]"), hasItem("2"));
    }
}
