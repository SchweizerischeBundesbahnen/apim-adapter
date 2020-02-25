package ch.sbb.integration.api.adapter.service.utils;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;


public class ErrorResponseProdModeHelperTest {

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-junit-prodMode.yml");
    }

    @Test
    public void createErrorAuthResponse() {
        String path = "/v1/locations";
        HttpMethod method = HttpMethod.GET;
        String clientId = "187e4s30";
        ErrorReason reason = ErrorReason.PATH_NOT_FOUND;
        String expectedMessage = "forbidden";

        ApimAdapterConfig apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        ErrorResponseHelper errorResponseHelper = new ErrorResponseHelper(apimAdapterConfig);

        AuthRepResponse generatedAuthResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, clientId, reason, path, null, method, emptyList());

        assertEquals(expectedMessage, generatedAuthResponse.getMessage());
        assertEquals(403, generatedAuthResponse.getHttpStatus());
    }

    @Test
    public void createErrorAuthResponseFor401() {
        String path = "/v1/locations";
        HttpMethod method = HttpMethod.GET;
        String clientId = "187e4s30";
        ErrorReason reason = ErrorReason.EXPIRED_OR_INVALID;
        String expectedMessage = ErrorReason.EXPIRED_OR_INVALID.getMessage();

        ApimAdapterConfig apimAdapterConfig = ApimAdapterFactory.createApimAdapterConfig();
        ErrorResponseHelper errorResponseHelper = new ErrorResponseHelper(apimAdapterConfig);

        AuthRepResponse generatedAuthResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, clientId, reason, path, null, method, emptyList());

        assertEquals(expectedMessage, generatedAuthResponse.getMessage());
        assertEquals(401, generatedAuthResponse.getHttpStatus());
    }

}