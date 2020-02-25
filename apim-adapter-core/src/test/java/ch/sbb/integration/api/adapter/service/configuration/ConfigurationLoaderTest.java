package ch.sbb.integration.api.adapter.service.configuration;

import ch.sbb.integration.api.adapter.model.ThreeScalePlanResult;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleAdminCommunicationComponent;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static ch.sbb.integration.api.adapter.model.ConfigType.*;
import static ch.sbb.integration.api.adapter.model.usage.ClientSyncState.OK;
import static ch.sbb.integration.api.adapter.model.usage.ClientSyncState.SERVER_ERROR;
import static ch.sbb.integration.api.adapter.util.Utilities.loadTextFromResource;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationLoaderTest {

    private static final String SERVICE_ID = "serviceId";
    private static final String CLIENT_ID = "clientID";
    private ConfigurationLoader testee;

    @Mock
    private ThreeScaleAdminCommunicationComponent mockedThreeScaleAdminCommunicationComponent;
    @Mock
    private ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent;
    @Mock
    private EmergencyModeState mockedEmergencyModeState;
    @Mock
    private OfflineConfigurationCacheRepo mockedOfflineConfigurationCacheRepo;

    @Before
    public void setup() {
        testee = new ConfigurationLoader(mockedOfflineConfigurationCacheRepo, mockedEmergencyModeState, mockedThreeScaleAdminCommunicationComponent, threeScaleBackendCommunicationComponent);
    }

    @Test
    public void successfulLoadPlanConfiguration() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        String xml = loadTextFromResource("stubs/plan/watchdog-200.xml");
        when(threeScaleBackendCommunicationComponent.loadThreeScalePlan(eq(CLIENT_ID))).thenReturn(new ThreeScalePlanResult(xml, HttpResponseCodes.SC_OK));

        //Act
        Client client = testee.loadPlanConfig(SERVICE_ID, CLIENT_ID, false);

        //Assert
        assertThat(client.getSyncState(), is(OK));
    }

    @Test
    public void ExceptionWhileLoadPlanConfiguration() throws ParserConfigurationException, SAXException, IOException {
        //Arrange
        when(threeScaleBackendCommunicationComponent.loadThreeScalePlan(eq(CLIENT_ID))).thenThrow(new RuntimeException());

        //Act
        Client client = testee.loadPlanConfig(SERVICE_ID, CLIENT_ID, false);

        //assert
        assertThat(client.getSyncState(), is(SERVER_ERROR));
    }

    @Test
    public void exceptionWhileLoadPlanConfigurationWithCache() throws IOException, ParserConfigurationException, SAXException {
        //Arrange
        when(threeScaleBackendCommunicationComponent.loadThreeScalePlan(eq(CLIENT_ID))).thenThrow(new RuntimeException());
        String xml = loadTextFromResource("stubs/plan/watchdog-200.xml");
        when(mockedOfflineConfigurationCacheRepo.findPlanConfig(eq(SERVICE_ID), eq(CLIENT_ID))).thenReturn(xml);

        //Act
        Client client = testee.loadPlanConfig(SERVICE_ID, CLIENT_ID, true);

        //Assert
        verify(mockedEmergencyModeState).setConfigurationSuccessfulLoaded(eq(PLAN), eq(false));
        assertThat(client.getSyncState(), is(OK));
    }

    @Test
    public void load503WhileLoadPlanConfigurationWithCache() throws IOException, ParserConfigurationException, SAXException {
        //Arrange
        String xml = loadTextFromResource("stubs/plan/watchdog-200.xml");
        when(threeScaleBackendCommunicationComponent.loadThreeScalePlan(eq(CLIENT_ID))).thenReturn(new ThreeScalePlanResult(xml, HttpResponseCodes.SC_SERVICE_UNAVAILABLE));
        when(mockedOfflineConfigurationCacheRepo.findPlanConfig(eq(SERVICE_ID), eq(CLIENT_ID))).thenReturn(xml);

        //Act
        Client client = testee.loadPlanConfig(SERVICE_ID, CLIENT_ID, true);

        //Assert
        verify(mockedEmergencyModeState).setConfigurationSuccessfulLoaded(eq(PLAN), eq(false));
        assertThat(client.getSyncState(), is(OK));
    }

    @Test
    public void successfulLoadMetricConfiguration() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadMetricConfig(eq(SERVICE_ID))).thenReturn("{\"id\": 1}");

        //Act
        JsonNode jsonNode = testee.loadMetricConfig(SERVICE_ID, false);

        //Assert
        assertThat(jsonNode.get("id").asText(), is("1"));
    }

    @Test(expected = RuntimeException.class)
    public void exceptionWhileLoadMetricConfiguration() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadMetricConfig(eq(SERVICE_ID))).thenThrow(new RuntimeException());

        //Act
        testee.loadMetricConfig(SERVICE_ID, false);
    }

    @Test
    public void exceptionWhileLoadMetricConfigurationWithCache() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadMetricConfig(eq(SERVICE_ID))).thenThrow(new RuntimeException());
        when(mockedOfflineConfigurationCacheRepo.findMetricConfig(eq(SERVICE_ID))).thenReturn("{\"id\": 1}");

        //Act
        JsonNode jsonNode = testee.loadMetricConfig(SERVICE_ID, true);

        //Assert
        verify(mockedEmergencyModeState).setConfigurationSuccessfulLoaded(eq(METRIC), eq(false));
        assertThat(jsonNode.get("id").asText(), is("1"));

    }

    @Test
    public void successfulLoadMappingRulesConfiguration() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadMappingRulesConfig((eq(SERVICE_ID)))).thenReturn("{\"mapping_rules\" : {\"id\": 1}}");

        //Act
        JsonNode jsonNode = testee.loadMappingRulesConfig(SERVICE_ID, false);

        //Assert
        assertThat(jsonNode.get("id").asText(), is("1"));
    }

    @Test(expected = RuntimeException.class)
    public void exceptionWhileloadMappingRulesConfiguration() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadMappingRulesConfig((eq(SERVICE_ID)))).thenThrow(new RuntimeException());

        //Act
        testee.loadMappingRulesConfig(SERVICE_ID, false);
    }

    @Test
    public void exceptionWhileloadMappingRulesConfigurationWithCache() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadMappingRulesConfig((eq(SERVICE_ID)))).thenThrow(new RuntimeException());
        when(mockedOfflineConfigurationCacheRepo.findMappingRulesConfig((eq(SERVICE_ID)))).thenReturn("{\"mapping_rules\" : {\"id\": 1}}");

        //Act
        JsonNode jsonNode = testee.loadMappingRulesConfig(SERVICE_ID, true);

        //Assert
        verify(mockedEmergencyModeState).setConfigurationSuccessfulLoaded(eq(MAPPING_RULES), eq(false));
        assertThat(jsonNode.get("id").asText(), is("1"));
    }

    @Test
    public void successfulLoadProxyConfiguration() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadProxyConfig(eq(SERVICE_ID))).thenReturn("{\"id\": 1}");

        //Act
        JsonNode jsonNode = testee.loadProxyConfig(SERVICE_ID, false);

        //Assert
        assertThat(jsonNode.get("id").asText(), is("1"));
    }

    @Test(expected = RuntimeException.class)
    public void exceptionWhileloadProxyConfigConfiguration() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadProxyConfig(eq(SERVICE_ID))).thenThrow(new RuntimeException());

        //Act
        testee.loadProxyConfig(SERVICE_ID, false);
    }

    @Test
    public void exceptionWhileloadProxyConfigConfigurationWithCache() {
        //Arrange
        when(mockedThreeScaleAdminCommunicationComponent.loadProxyConfig(eq(SERVICE_ID))).thenThrow(new RuntimeException());
        when(mockedOfflineConfigurationCacheRepo.findProxyConfig(eq(SERVICE_ID))).thenReturn("{\"id\": 1}");

        //Act
        JsonNode jsonNode = testee.loadProxyConfig(SERVICE_ID, true);

        //Assert
        verify(mockedEmergencyModeState).setConfigurationSuccessfulLoaded(eq(PROXY), eq(false));
        assertThat(jsonNode.get("id").asText(), is("1"));
    }
}