package ch.sbb.integration.api.adapter.service;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.factory.ApimAdapterFactory;
import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.service.utils.AuthUtils;
import ch.sbb.integration.api.adapter.service.utils.ErrorReason;
import ch.sbb.integration.api.adapter.service.utils.ErrorResponseHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ch.sbb.integration.api.adapter.service.utils.HttpMethod.GET;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by u217269 on 19.02.2018.
 */
@Ignore

//TODO fix with same configuration like watchdog
public class ThreeScaleAdapterServiceIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleAdapterServiceIntegrationTest.class);

    private ApimAdapterConfig config = ApimAdapterFactory.createApimAdapterConfig();
    private ApimAdapterService service = ApimAdapterFactory.createApimAdapterService();
    private ErrorResponseHelper errorResponseHelper = new ErrorResponseHelper(config);
    
    private int numberOfRuns = 10;

    @BeforeClass
    public static void init() {
        System.setProperty("threescale.properties.file", "/threescale-integrationtest.yml");
        
    }

    @Test
    public void integrationTestAuthRep() throws IOException {
    	RestConfig restConfig = new RestConfig();
        String tokenResponse = restConfig.newRestEasyClient()
                .target("https://sso-int.sbb.ch/auth/realms/SBB_Public/protocol/openid-connect/token")
                .request()
                .post(
                        Entity.entity("grant_type=client_credentials&client_id=gateway-integrationtest-int&client_secret=c5e97332-e0c9-404c-b41f-7200012723ed", MediaType.APPLICATION_FORM_URLENCODED),
                        String.class);

        String token = new ObjectMapper().readTree(tokenResponse).get("access_token").asText();
        LOG.info("Token=" + token);
        long currentNano;
        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < numberOfRuns; i++) {
            currentNano = System.nanoTime();
            AuthRepResponse response = service.authRep(token, "/v1/locations", GET);
            long duration = System.nanoTime() - currentNano;
            LOG.info("Time spent in milliseconds: " + (duration / 1000000));
            LOG.info(response.toString());
            //We expect a 403, but this is after the token is validated!
            AuthRepResponse expectedBadPathResponse = errorResponseHelper.createErrorAuthResponse(AuthUtils.RH_SSO_REALM, "gateway-integrationtest-int", ErrorReason.CLIENT_ID_HAS_NO_PERMISSION, "/v1/locations", null, GET, emptyList());
            assertFalse("Access must be denied", response.isAllowed());
            assertEquals(expectedBadPathResponse.getHttpStatus(), response.getHttpStatus());
            assertEquals(expectedBadPathResponse.getMessage(), response.getMessage());
            assertEquals(expectedBadPathResponse.getClientId(), response.getClientId());
            durations.add(duration);
        }
    }

}

