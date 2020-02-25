package ch.sbb.integration.api.adapter.service.utils;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ErrorResponseHelperTest {

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-junit.yml");
    }

    @Test
    public void createErrorAuthResponse() {
        String path = "/v1/locations";
        HttpMethod method = HttpMethod.GET;
        String clientId = "187e4s30";
        ErrorReason reason = ErrorReason.LIMIT_EXCEEDED;
        String expectedMessage = ErrorResponseHelper.createExtendedMessage(reason.getMessage(), path, "", method);

        ApimAdapterConfig apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        ErrorResponseHelper errorResponseHelper = new ErrorResponseHelper(apimAdapterConfig);
        AuthRepResponse generatedAuthResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, clientId, reason, path, "", method, emptyList());

        assertEquals(clientId, generatedAuthResponse.getClientId());
        assertEquals(reason.getHttpStatus(), generatedAuthResponse.getHttpStatus());
        assertEquals(expectedMessage, generatedAuthResponse.getMessage());
        assertFalse(generatedAuthResponse.isAllowed());
    }

    @Test
    public void createExtendedMessage() {
        String path = "/v1/locations";
        String queryString = "query=String";
        HttpMethod method = HttpMethod.GET;
        ErrorReason reason = ErrorReason.PATH_NOT_FOUND;


        String message = ErrorResponseHelper.createExtendedMessage(reason.getMessage(), path, queryString, method);

        assertNotNull(message);
        assertTrue(message.contains(reason.getMessage()));
        assertTrue(message.contains(method.toString()));
        assertTrue(message.contains(queryString));
        assertTrue(message.contains(path));

    }
}