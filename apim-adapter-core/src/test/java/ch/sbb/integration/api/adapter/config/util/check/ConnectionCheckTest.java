package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.model.ConfigType;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConnectionCheckTest extends AbstractWiremockTest {


    private String baseUrl = "http://localhost:8099";

    @Before
    public void initStubs() {
        WireMock.reset();
        StubGenerator.instantiateAll();
    }

    @Test
    public void checkURIfor4xxOr200() {
    	ConnectionCheck connectionCheck = new ConnectionCheck(apimAdapterConfig, emergencyModeState);
    	
    	CheckResult check200 = connectionCheck.checkURIfor401or403(baseUrl + "/admin/api/services/.*/proxy/mapping_rules.json.*");
        CheckResult check400 = connectionCheck.checkURIfor401or403(baseUrl + "/YouShallNotPass");
        CheckResult check500 = connectionCheck.checkURIfor401or403(baseUrl + "/misterStarkIDontFeelSoGood");

    

        assertFalse(check200.isUp());
        assertEquals(Status.DOWN, check200.getStatus());

        assertTrue(check400.isUp());
        assertEquals(Status.UP, check400.getStatus());

        assertFalse(check500.isUp());
        assertEquals(Status.DOWN, check500.getStatus());

    }

    @Test
    public void checkURI() {
    	ConnectionCheck connectionCheck = new ConnectionCheck(apimAdapterConfig, emergencyModeState);
    	
        CheckResult check200 = connectionCheck.checkURI(baseUrl + "/admin/api/services/.*/proxy/mapping_rules.json.*");
        CheckResult check400 = connectionCheck.checkURI(baseUrl + "/YouShallNotPass");
        CheckResult check500 = connectionCheck.checkURI(baseUrl + "/misterStarkIDontFeelSoGood");


        assertTrue(check200.isUp());
        assertEquals(Status.UP, check200.getStatus());

        assertFalse(check400.isUp());
        assertEquals(Status.DOWN, check400.getStatus());

        assertFalse(check500.isUp());
        assertEquals(Status.DOWN, check500.getStatus());

    }


    @Test
    public void checkReadTimeout() {
        ConnectionCheck connectionCheck = new ConnectionCheck(apimAdapterConfig, emergencyModeState, 500, 500);

        StubGenerator.instantiateStubWith20SecondsDelay();

        CheckResult check = connectionCheck.checkURI(baseUrl + "/iWillWaitHere");

        assertFalse(check.isUp());
        assertEquals(Status.DOWN, check.getStatus());
    }

    @Test
    public void connectionCheckTestInEmergencyMode() {
        EmergencyModeState emergencyModeState = new EmergencyModeState();
        // config type is irrelevant here, one is not in success, emergency mode is on
        emergencyModeState.setConfigurationSuccessfulLoaded(ConfigType.PROXY, false);

        // this would lead to a read timeout - but due to EmergencyModeState#isEmergencyMode() this is skipped
        StubGenerator.instantiateStubWith20SecondsDelay();

        ConnectionCheck connectionCheck = new ConnectionCheck(apimAdapterConfig, emergencyModeState);
        CheckResult check = connectionCheck.checkURI(baseUrl + "/iWillWaitHere");

        assertTrue(check.isUp());
        assertEquals(Status.UP, check.getStatus());
    }
}