package ch.sbb.integration.api.adapter.service.job;

import ch.sbb.integration.api.adapter.model.Metric;
import ch.sbb.integration.api.adapter.model.TransactionsRequest;
import ch.sbb.integration.api.adapter.model.reporting.Hits;
import ch.sbb.integration.api.adapter.model.reporting.ResponseSummary;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.model.usage.ClientSyncState;
import ch.sbb.integration.api.adapter.model.usage.MetricUsage;
import ch.sbb.integration.api.adapter.service.cache.ClientCache;
import ch.sbb.integration.api.adapter.service.cache.ServiceToMetricsCache;
import ch.sbb.integration.api.adapter.service.configuration.ConfigurationLoader;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ThreeScaleSynchronizerServiceTest {
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String METRIC_ID = "METRIC_ID";
    public static final ResponseSummary RESPONSE_SUMMARY = new ResponseSummary(CLIENT_ID, 200, METRIC_ID);

    private ThreeScaleSynchronizerService testee;

    @Mock
    private ClientCache mockedClientCache;
    @Mock
    private ServiceToMetricsCache mockedServiceToMetricsCache;
    @Mock
    private ConfigurationLoader mockedConfigurationLoader;
    @Mock
    private ThreeScaleBackendCommunicationComponent mockedThreeScaleBackendCommunicationComponent;
    @Mock
    private EmergencyModeState mockedEmergencyModeState;

    @Captor
    private ArgumentCaptor<Map<ResponseSummary, Long>> captor;

    private List<String> clientIds = new ArrayList<>();

    private Hits hits;

    @Before
    public void setUp() {
        when(mockedClientCache.clientIds()).thenReturn(clientIds);

        hits = new Hits();
        testee = new ThreeScaleSynchronizerService(
                mockedClientCache,
                mockedServiceToMetricsCache,
                null,
                mockedConfigurationLoader,
                mockedThreeScaleBackendCommunicationComponent,
                mockedEmergencyModeState,
                hits);
    }

    @Test
    public void noClient_isNotReported(){
        //Act
        testee.run();

        //Assert
        verifyZeroInteractions(mockedThreeScaleBackendCommunicationComponent);
    }

    @Test
    public void clientWithNoUsage_isNotReported(){
        clientIds.add(CLIENT_ID);

        //Act
        testee.run();

        //Assert
        verifyZeroInteractions(mockedThreeScaleBackendCommunicationComponent);
    }

    @Test
    public void unlimitedMetric_areReportedOnce() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any())).thenReturn(true);

        Map<String, MetricUsage> usageMap = new HashMap<>();
        MetricUsage metricUsage = MetricUsage.unlimitedMetric(CLIENT_ID, METRIC_ID);

        usageMap.put(METRIC_ID, metricUsage);
        Client chachedClient = new Client(CLIENT_ID, usageMap, ClientSyncState.OK);
        Client newClient = new Client(CLIENT_ID, usageMap, ClientSyncState.OK);
        mockCachedAndNewClient(chachedClient, newClient);

        //Act 0
        hits.addUnreportHits(RESPONSE_SUMMARY, 1L);

        //Act 1
        testee.run();

        //Assert 1
        assertThat(hits.extractUnreportedHits().isEmpty(), is(true));
        assertThat(metricUsage.getCurrentUsage().get(), is(0L));

        verify(mockedThreeScaleBackendCommunicationComponent).report(captor.capture());
        assertSingleTransactions(captor.getValue());

        //Act 2
        //We sync again
        testee.run();

        //Assert 2
        //No more reporting
        verifyNoMoreInteractions(mockedThreeScaleBackendCommunicationComponent);
    }

    @Test
    public void limitedMetrics_areReportedOnce() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any())).thenReturn(true);
        prepareMetricUsage(now().minusSeconds(1), now());

        //Act 0
        hits.addUnreportHits(RESPONSE_SUMMARY, 1L);

        //Act 1
        testee.run();

        //Assert 1
        assertThat(hits.extractUnreportedHits().size(), is(0));
        verify(mockedThreeScaleBackendCommunicationComponent).report(captor.capture());
        assertSingleTransactions(captor.getValue());

        //Act 2
        //We sync again
        testee.run();

        //Assert 2
        //No more reporting
        verifyNoMoreInteractions(mockedThreeScaleBackendCommunicationComponent);
    }


    @Test
    public void limitedMetrics_ChangeUsage() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any())).thenReturn(true);

        Map<String, MetricUsage> chachedUsageMap = new HashMap<>();
        long cachedBase = 10L;
        long chachedLimit = 5L;

        MetricUsage chachedMetricUsage = MetricUsage.limitedMetric(CLIENT_ID, METRIC_ID, chachedLimit, cachedBase, toDateTimeString(now().minusHours(1)), toDateTimeString(now().plusHours(1)));
        chachedUsageMap.put(METRIC_ID, chachedMetricUsage);
        Client chachedClient = new Client(CLIENT_ID, chachedUsageMap, ClientSyncState.OK);

        Map<String, MetricUsage> newUsageMap = new HashMap<>();
        long newLimit = 20L;
        long newBase = 15L;
        MetricUsage newMetricUsage = MetricUsage.limitedMetric(CLIENT_ID, METRIC_ID, newLimit, newBase, toDateTimeString(now().minusHours(1)), toDateTimeString(now().plusHours(1)));
        newUsageMap.put(METRIC_ID, newMetricUsage);
        Client newClient = new Client(CLIENT_ID, newUsageMap, ClientSyncState.OK);

        mockCachedAndNewClient(chachedClient, newClient);

        when(mockedServiceToMetricsCache.get(any())).thenReturn(singletonList(new Metric(METRIC_ID, METRIC_ID, METRIC_ID, Collections.emptyList())));


        //Act 0
        chachedMetricUsage.incrementCurrentUsage(); //We hit the metric once

        assertThat(chachedClient.getUsage(METRIC_ID).getLimit(), is(chachedLimit));
        assertThat(chachedClient.getUsage(METRIC_ID).getBase(), is(cachedBase));

        //Act 1
        testee.run();

        //Assert 1
        assertThat(chachedClient.getUsage(METRIC_ID).getLimit(), is(newLimit));
        assertThat(chachedClient.getUsage(METRIC_ID).getBase(), is(newBase));
    }

    @Test
    public void limitedMetrics_deleteMetric() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any())).thenReturn(true);

        Map<String, MetricUsage> chachedUsageMap = new HashMap<>();
        long cachedBase = 10L;
        long chachedLimit = 5L;

        MetricUsage chachedMetricUsage = MetricUsage.limitedMetric(CLIENT_ID, METRIC_ID, chachedLimit, cachedBase, toDateTimeString(now().minusHours(1)), toDateTimeString(now().plusHours(1)));
        chachedUsageMap.put(METRIC_ID, chachedMetricUsage);
        Client chachedClient = new Client(CLIENT_ID, chachedUsageMap, ClientSyncState.OK);

        mockCachedAndNewClient(chachedClient, chachedClient);
        when(mockedServiceToMetricsCache.get(any())).thenReturn(emptyList());

        //Act 0
        chachedMetricUsage.incrementCurrentUsage(); //We hit the metric once

        assertThat(chachedClient.getUsage(METRIC_ID).getLimit(), is(chachedLimit));
        assertThat(chachedClient.getUsage(METRIC_ID).getBase(), is(cachedBase));

        //Act 1
        testee.run();

        //Assert 1
        assertThat(chachedClient.getUsage(METRIC_ID), is(IsNull.nullValue()));
    }

    @Test
    public void hitsOnlimitedMetrics_areReportedOnce_in500Case() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any()))
                .thenReturn(false) //There is a error on first report
                .thenReturn(true)                    //ok on 2nd report
                .thenReturn(true);                   //ok on 3nd report

        MetricUsage metricUsage = prepareMetricUsage(now().minusSeconds(1), now());

        //Act 0
        hits.addUnreportHits(RESPONSE_SUMMARY, 1L);
        metricUsage.incrementCurrentUsage(); //We hit the metric once

        assertThat(metricUsage.getCurrentUsage().get(), is(1L));

        //Act 1
        testee.run();

        //Assert 1
        assertThat(metricUsage.getCurrentUsage().get(), is(1L)); //Still one count, because nothing was reported

        //We got this hit, but it was returned with 500.
        verify(mockedThreeScaleBackendCommunicationComponent).report(captor.capture());
        assertSingleTransactions(captor.getValue());

        //Act 2
        //We sync again
        testee.run();

        //Assert 2
        //No more reporting
        verify(mockedThreeScaleBackendCommunicationComponent, times(2)).report(captor.capture());
        assertSingleTransactions(captor.getAllValues().get(1));

        //Act 3
        //We sync again
        testee.run();

        //Assert 3
        //No more reporting
        verifyNoMoreInteractions(mockedThreeScaleBackendCommunicationComponent);
    }

    private void assertSingleTransactions(List<TransactionsRequest> transactions) {
        assertThat(transactions.size(), is(1));
        assertThat(transactions.get(0).getAppId(), is(CLIENT_ID));
        assertThat(transactions.get(0).getUsage().size(), is(1));
        assertThat(transactions.get(0).getUsage().get(METRIC_ID), is(1L));
    }

    private void assertSingleTransactions(Map<ResponseSummary, Long> hits) {
        assertThat(hits.size(), is(1));
        assertThat(hits.containsKey(RESPONSE_SUMMARY), is(true));
        assertThat(hits.get(RESPONSE_SUMMARY), is(1L));
    }

    @Test
    public void hitsFromYesterdayOnlimitedMetrics_areNotReportedOnce_in500Case() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any()))
                .thenReturn(false) //There is a error on first report
                .thenReturn(true)                    //ok on 2nd report
                .thenReturn(true);                   //ok on 3nd report

        MetricUsage metricUsage = prepareMetricUsage(now().minusDays(2).minusSeconds(1), now().minusDays(2));

        //Act 0
        metricUsage.incrementCurrentUsage(); //We hit the metric once
        assertThat(metricUsage.getCurrentUsage().get(), is(1L));

        //Act 1
        testee.run();

        //Assert 1
        assertThat(metricUsage.getCurrentUsage().get(), is(1L)); //Still one count, because nothing was reported

        verifyZeroInteractions(mockedThreeScaleBackendCommunicationComponent);
    }

    @Test
    public void hitsFromTodayOnlimitedMetrics_areReportedOnce_in500Case() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        List<Metric> metrics = singletonList(new Metric(METRIC_ID, METRIC_ID, null, null));
        when(mockedServiceToMetricsCache.get(any())).thenReturn(metrics);
        when(mockedThreeScaleBackendCommunicationComponent.report(Matchers.any()))
                .thenReturn(false) //There is a error on first report
                .thenReturn(true)                    //ok on 2nd report
                .thenReturn(true);                   //ok on 3nd report

        ZonedDateTime from = now().minusHours(1).minusSeconds(1);
        ZonedDateTime to = now().minusHours(1);

        MetricUsage metricUsage = prepareMetricUsage(from, to);

        //Act 0
        hits.addUnreportHits(RESPONSE_SUMMARY, 1L);

        //Act 1
        testee.run();

        //Assert 1
        verify(mockedThreeScaleBackendCommunicationComponent).report(captor.capture());
        assertSingleTransactions(captor.getValue());

        //Act 2
        //We sync again
        testee.run();

        //Assert 2
        //No more reporting
        verify(mockedThreeScaleBackendCommunicationComponent, times(2)).report(captor.capture());
        assertSingleTransactions(captor.getAllValues().get(1));

        //Act 3
        //We sync again
        testee.run();

        //Assert 3
        //No more reporting
        verifyNoMoreInteractions(mockedThreeScaleBackendCommunicationComponent);
    }

    private MetricUsage prepareMetricUsage(ZonedDateTime from, ZonedDateTime to) throws IOException, SAXException, ParserConfigurationException {
        Map<String, MetricUsage> usageMap = new HashMap<>();
        MetricUsage metricUsage = MetricUsage.limitedMetric(CLIENT_ID, METRIC_ID, 10L, 10L, toDateTimeString(from), toDateTimeString(to));

        usageMap.put(METRIC_ID, metricUsage);
        Client chachedClient = new Client(CLIENT_ID, usageMap, ClientSyncState.OK);
        Client newClient = new Client(CLIENT_ID, usageMap, ClientSyncState.OK);

        mockCachedAndNewClient(chachedClient, newClient);
        return metricUsage;
    }

    private void mockCachedAndNewClient(Client chachedClient, Client newClient) throws IOException, SAXException, ParserConfigurationException {
        clientIds.add(CLIENT_ID);
        when(mockedClientCache.get(eq(CLIENT_ID))).thenReturn(chachedClient);
        when(mockedConfigurationLoader.loadPlanConfig(any(String.class), eq(CLIENT_ID), eq(false))).thenReturn(newClient);
    }

    private String toDateTimeString(ZonedDateTime lastSecond) {
        return lastSecond.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
    }
}