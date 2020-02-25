package ch.sbb.integration.api.adapter.filter;

import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.util.TokenGenerator;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

import static ch.sbb.integration.api.adapter.service.utils.AuthUtils.HTTP_AUTHORIZATION_HEADER_NAME;
import static ch.sbb.integration.api.adapter.service.utils.AuthUtils.mapJwtToHttpAuthorizationHeaderValue;
import static ch.sbb.integration.api.adapter.wiremock.UrlMappingRules.patternForAuthorization;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApimAdapterFilterTest extends AbstractWiremockTest {

	private MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
	private MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
	private MockFilterChain mockFilterChain = new MockFilterChain();
	
	private ApimAdapterFilter filter;
	
	@Before
    public void initStubs() {
		WireMock.reset();
        StubGenerator.instantiateAll();
		ApimAdapterService service = ApimAdapterFactory.createApimAdapterService();
        filter = new ApimAdapterFilter(service, Collections.emptyList());
		
    }
	
	@Test
	public void testFilterSuccess() throws IOException, ServletException {
		mockHttpServletRequest.setRequestURI(V1_LOCATIONS);
		mockHttpServletRequest.setMethod(HttpMethod.GET.toString());
		String token = TokenGenerator.getInstance().generateBearerToken(CLIENT_ID, 30_000);
		mockHttpServletRequest.addHeader(HTTP_AUTHORIZATION_HEADER_NAME, mapJwtToHttpAuthorizationHeaderValue(token));
		
		stubFor(get(patternForAuthorization())
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(StubGenerator.authResponseForAnUnlimitedPlan())));
	
		filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
			
		assertNotNull(mockHttpServletResponse.getOutputStream());
		assertEquals(200, mockHttpServletResponse.getStatus());
		assertEquals(null, mockHttpServletResponse.getErrorMessage());
	}
	
	@Test
	public void testFilterFailure() throws IOException, ServletException {
		mockHttpServletRequest.setRequestURI(V1_LOCATIONS);
		mockHttpServletRequest.setMethod(HttpMethod.GET.toString());
		String token = TokenGenerator.getInstance().generateBearerToken("invalidClientId", 30_000);
		mockHttpServletRequest.addHeader(HTTP_AUTHORIZATION_HEADER_NAME, mapJwtToHttpAuthorizationHeaderValue(token));
		
		stubFor(get(patternForAuthorization())
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_FORBIDDEN)
                        .withBody(StubGenerator.authResponseForAnInvalidClientId())));
	
		filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
			
		assertNotNull(mockHttpServletResponse.getOutputStream());
		assertEquals(403, mockHttpServletResponse.getStatus());
		assertTrue(mockHttpServletResponse.getErrorMessage().contains("Client has no permission"));
	}

}
