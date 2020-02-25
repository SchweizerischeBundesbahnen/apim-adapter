package ch.sbb.integration.api.gateway.handler;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.apiwatch.ApiWatch;
import ch.sbb.integration.api.gateway.ApimSingleton;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static io.undertow.util.StatusCodes.UNAUTHORIZED;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThreeScaleAdapterHandlerTest {

    private ThreeScaleAdapterHandler testee;

    private HttpServerExchange exchange;

    @Mock
    private Sender mockedResponseSender;

    @Mock
    private ApimAdapterService mockedApimAdapterService;

    @Mock
    private ApiWatch mockedApiWatch;

    @Mock
    private ApimAdapterConfig adapterConfig;

    private HeaderMap requestHeaderMap = new HeaderMap();
    private HeaderMap responseHeaderMap = new HeaderMap();

    @Before
    public void setup() {
        exchange = mock(HttpServerExchange.class);
        when(exchange.getResponseSender()).thenReturn(mockedResponseSender);
        when(exchange.getRequestHeaders()).thenReturn(requestHeaderMap);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaderMap);

        ApimSingleton.set(mockedApimAdapterService);
        ApimSingleton.setAdapterConfig(adapterConfig);

        testee = new ThreeScaleAdapterHandler();
    }

    @Test
    public void whenErrorFromBackendNoOrigin_ThenNoCorsReturned() {
        //Arrange
        mockUnauthorized();

        //Act
        testee.handleRequest(exchange);

        //Assert
        verify(exchange).setStatusCode(eq(UNAUTHORIZED));
        assertThat(responseHeaderMap.size(), is(0));
    }

    @Test
    public void whenErrorFromBackendWithOrigin_ThenCorsReturned() {
        //Arrange
        mockUnauthorized();
        String origin = "http://www.google.ch";
        requestHeaderMap.add(HttpString.tryFromString("origin"), origin);

        //Act
        testee.handleRequest(exchange);

        //Assert
        verify(exchange).setStatusCode(eq(UNAUTHORIZED));
        assertThat(responseHeaderMap.size(), is(1));
        assertThat(responseHeaderMap.get(HttpString.tryFromString("Access-Control-Allow-Origin")).getFirst(), is(origin));
    }

    private void mockUnauthorized() {
        AuthRepResponse mockedResponse = mock(AuthRepResponse.class);
        when(mockedResponse.isAllowed()).thenReturn(false);
        when(mockedResponse.getHttpStatus()).thenReturn(UNAUTHORIZED);
        when(mockedApimAdapterService.getApiWatch()).thenReturn(mockedApiWatch);
        when(mockedApimAdapterService.authRep(isNull(), isNull(), isNull(), isNull())).thenReturn(mockedResponse);
    }
}