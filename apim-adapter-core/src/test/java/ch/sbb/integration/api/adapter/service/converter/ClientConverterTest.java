package ch.sbb.integration.api.adapter.service.converter;

import ch.sbb.integration.api.adapter.model.usage.Client;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static ch.sbb.integration.api.adapter.model.usage.ClientSyncState.*;
import static ch.sbb.integration.api.adapter.util.Utilities.loadTextFromResource;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.jboss.resteasy.util.HttpResponseCodes.*;
import static org.junit.Assert.assertThat;

public class ClientConverterTest {

    public static final String CLIENT_ID = "CLIENT_ID";
    private ClientConverter testee;

    @Before
    public void setup() {
        testee = new ClientConverter();
    }

    @Test
    public void convert200Ok() throws IOException, ParserConfigurationException, SAXException {
        //Arrange
        String text = loadTextFromResource("stubs/plan/watchdog-200.xml");

        //Act
        Client client = testee.convertToClient(CLIENT_ID, SC_OK, text);

        //Assert
        assertThat(client.getId(), is(CLIENT_ID));
        assertThat(client.getSyncState(), is(OK));
        assertThat(client.getUsage("hits").getMetricSysName(), is("hits"));
    }

    @Test
    public void convert409Conflict() throws IOException, ParserConfigurationException, SAXException {
        //Arrange
        String text = loadTextFromResource("stubs/plan/watchdog-409.xml");

        //Act
        Client client = testee.convertToClient(CLIENT_ID, SC_CONFLICT, text);

        //Assert
        assertThat(client.getId(), is(CLIENT_ID));
        assertThat(client.getSyncState(), is(USAGE_LIMITS_EXCEEDED));
        assertThat(client.getUsage("hits").getMetricSysName(), is("hits"));
    }

    @Test
    public void convert500ServerError() throws IOException, ParserConfigurationException, SAXException {
        //Arrange

        //Act
        Client client = testee.convertToClient(CLIENT_ID, SC_SERVICE_UNAVAILABLE, "");

        //Assert
        assertThat(client.getId(), is(CLIENT_ID));
        assertThat(client.getSyncState(), is(SERVER_ERROR));
        assertThat(client.getUsage("hits"), is(nullValue()));
    }

    @Test
    public void convert200Empty() throws IOException, ParserConfigurationException, SAXException {
        //Arrange

        //Act
        Client client = testee.convertToClient(CLIENT_ID, SC_OK, "");

        //Assert
        assertThat(client.getId(), is(CLIENT_ID));
        assertThat(client.getSyncState(), is(UNKNOWN));
        assertThat(client.getUsage("hits"), is(nullValue()));
    }

    @Test
    public void convert404ApplicationNotFound() throws IOException, ParserConfigurationException, SAXException {
        //Arrange

        //Act
        Client client = testee.convertToClient(CLIENT_ID, SC_NOT_FOUND, "application_not_found");

        //Assert
        assertThat(client.getId(), is(CLIENT_ID));
        assertThat(client.getSyncState(), is(APPLICATION_NOT_FOUND));
        assertThat(client.getUsage("hits"), is(nullValue()));
    }
}