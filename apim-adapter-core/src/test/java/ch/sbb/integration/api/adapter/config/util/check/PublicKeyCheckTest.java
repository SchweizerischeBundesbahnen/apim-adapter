package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuerStore;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.wiremock.AbstractWiremockTest;
import ch.sbb.integration.api.adapter.wiremock.StubGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublicKeyCheckTest extends AbstractWiremockTest {
    private Path dir;

    @Before
    public void setup() throws IOException {
        dir = Files.createTempDirectory(PublicKeyCheckTest.class.getSimpleName());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(dir.toFile());
    }

    @Test
    public void check() {
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();

        TokenIssuerStore.reset();
        TokenIssuerStore.init(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());

        assertTrue(TokenIssuerStore.getInstance().getIssuers().size() > 0);

        // first up
        final PublicKeyCheck publicKeyCheck = new PublicKeyCheck();
        assertUp(publicKeyCheck.checkPublicKey());

        // then down
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithError();
        assertDown(publicKeyCheck.checkPublicKey());

        // then up again
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();
        assertUp(publicKeyCheck.checkPublicKey());
    }

    @Test
    public void checkJwksDownWithoutOfflineCache() {
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithError();

        TokenIssuerStore.reset();
        TokenIssuerStore.init(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());

        PublicKeyCheck publicKeyCheck = new PublicKeyCheck();
        assertDown(publicKeyCheck.checkPublicKey());
    }

    @Test
    public void checkOidcDownWithoutOfflineCache() {
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerWithError();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();

        TokenIssuerStore.reset();
        TokenIssuerStore.init(apimAdapterConfig, restConfig, OfflineConfigurationCacheRepo.disabled());

        PublicKeyCheck publicKeyCheck = new PublicKeyCheck();
        assertDown(publicKeyCheck.checkPublicKey());
    }

    @Test
    public void checkWithOfflineCache() {
        final OfflineConfigurationCacheRepo offlineConfigurationCacheRepo = new OfflineConfigurationCacheRepo(dir.toAbsolutePath().toString());

        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwks();

        TokenIssuerStore.reset();
        TokenIssuerStore.init(apimAdapterConfig, restConfig, offlineConfigurationCacheRepo);

        assertTrue(TokenIssuerStore.getInstance().getIssuers().size() > 0);

        // first up
        final PublicKeyCheck publicKeyCheck = new PublicKeyCheck();
        assertUp(publicKeyCheck.checkPublicKey());

        // then take down JWKS - check has to return "UP" as offline cache is present
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuer();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithError();
        assertUp(publicKeyCheck.checkPublicKey());

        // reset token issuer store again - but leave token issuer JWKS down - offline cache has to work
        TokenIssuerStore.reset();
        TokenIssuerStore.init(apimAdapterConfig, restConfig, offlineConfigurationCacheRepo);
        assertUp(publicKeyCheck.checkPublicKey());

        // reset token issuer store again - take oidc down as well
        WireMock.reset();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerWithError();
        StubGenerator.instantiateStubForSbbPublicTokenIssuerJwksWithError();
        TokenIssuerStore.reset();
        TokenIssuerStore.init(apimAdapterConfig, restConfig, offlineConfigurationCacheRepo);
        assertUp(publicKeyCheck.checkPublicKey());
    }

    private void assertUp(CheckResult checkResultStillUp2) {
        assertTrue(checkResultStillUp2.isUp());
        assertEquals(Status.UP, checkResultStillUp2.getStatus());
    }

    private void assertDown(CheckResult checkResultDown) {
        assertFalse(checkResultDown.isUp());
        assertEquals(Status.DOWN, checkResultDown.getStatus());
    }

}