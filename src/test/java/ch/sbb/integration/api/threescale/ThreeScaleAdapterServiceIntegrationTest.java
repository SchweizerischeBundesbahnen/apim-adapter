package ch.sbb.integration.api.threescale;

import ch.sbb.integration.api.threescale.config.RestConfig;
import ch.sbb.integration.api.threescale.model.AuthRepResponse;
import ch.sbb.integration.api.threescale.service.ThreeScaleAdapterService;
import ch.sbb.integration.api.threescale.service.utils.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * Created by u217269 on 19.02.2018.
 */
public class ThreeScaleAdapterServiceIntegrationTest {

    private static final Logger LOG = Logger.getLogger(ThreeScaleAdapterServiceIntegrationTest.class);

    private ThreeScaleAdapterService service = new ThreeScaleAdapterService();

    public static void main(String[] args) throws IOException {
        System.setProperty("threescale.properties.file", "/threescale-integrationtest.yml");
        new ThreeScaleAdapterServiceIntegrationTest().integrationTestAuthRep();
    }

    public void integrationTestAuthRep() throws IOException {

        String tokenResponse = RestConfig.newRestEasyClient()
                .target("https://sso.sbb.ch/auth/realms/SBB_Public/protocol/openid-connect/token")
                .request()
                .post(
                        Entity.entity("grant_type=client_credentials&client_id=084e8c30&client_secret=6887db2af03e42962a0a688da0986deb", MediaType.APPLICATION_FORM_URLENCODED),
                        String.class);

        String token = new ObjectMapper().readTree(tokenResponse).get("access_token").asText();
        LOG.info("Token=" + token);

        while (true) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long currentNano = System.nanoTime();
            AuthRepResponse response = service.authRep(token, "/locations", HttpMethod.GET);
            LOG.info("Time spent in nanoseconds: " + (System.nanoTime() - currentNano));
            LOG.info(response);
        }

    }

}
