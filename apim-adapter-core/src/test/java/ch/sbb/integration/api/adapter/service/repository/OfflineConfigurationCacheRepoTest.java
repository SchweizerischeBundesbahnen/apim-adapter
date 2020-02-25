package ch.sbb.integration.api.adapter.service.repository;

import ch.sbb.integration.api.adapter.model.ConfigType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo.DISABLED;
import static ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo.NUMBER_ROTATING_FILES;
import static ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo.configBaseFileName;
import static org.junit.Assert.*;

public class OfflineConfigurationCacheRepoTest {

    private Path dir;

    @Before
    public void setup() throws IOException {
        dir = Files.createTempDirectory(OfflineConfigurationCacheRepoTest.class.getSimpleName());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(dir.toFile());
    }

    private OfflineConfigurationCacheRepo createRepo() {
        return new OfflineConfigurationCacheRepo(dir.toAbsolutePath().toString());
    }

    private String createDummyConfig() {
        try {
            // sleep one millisecond here - so that there is no conflict when tests are executed to fast...
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
            // ignore
        }
        return "{\"prop\":\"value " + System.nanoTime() + "\"}";
    }

    @Test
    public void testPersistAndFindMappingRulesConfig() {
        OfflineConfigurationCacheRepo repo = createRepo();
        String config = createDummyConfig();
        assertTrue(repo.persistMappingRulesConfig("serviceId1", config));
        assertEquals(config, repo.findMappingRulesConfig("serviceId1"));
    }

    @Test
    public void testPersistAndFindMetricConfig() {
        OfflineConfigurationCacheRepo repo = createRepo();
        String config = createDummyConfig();
        assertTrue(repo.persistMetricConfig("serviceId1", config));
        assertEquals(config, repo.findMetricConfig("serviceId1"));
    }

    @Test
    public void testPersistAndFindProxyConfig() {
        OfflineConfigurationCacheRepo repo = createRepo();
        String config = createDummyConfig();
        assertTrue(repo.persistProxyConfig("serviceId1", config));
        assertEquals(config, repo.findProxyConfig("serviceId1"));
    }

    @Test
    public void testPersistAndFindPlanConfig() {
        OfflineConfigurationCacheRepo repo = createRepo();
        String config = createDummyConfig();
        assertTrue(repo.persistPlanConfig("serviceId1", "clientIdX", config));
        assertEquals(config, repo.findPlanConfig("serviceId1", "clientIdX"));
    }

    @Test
    public void testDeletePlanConfig() {
        OfflineConfigurationCacheRepo repo = createRepo();
        String config = createDummyConfig();
        repo.persistPlanConfig("serviceId1", "clientIdA", config);
        repo.persistPlanConfig("serviceId1", "clientIdA", config);
        repo.persistPlanConfig("serviceId1", "clientIdX", config);

        final String[] filesBeforeDelete = repo.dir(ConfigType.PLAN).toFile().list();
        assertEquals(3, filesBeforeDelete.length);

        repo.deletePlanConfig("serviceId1", "clientIdA");

        final String[] filesAfterFirstDelete = repo.dir(ConfigType.PLAN).toFile().list();
        assertEquals(1, filesAfterFirstDelete.length);
        assertFalse(filesAfterFirstDelete[0].contains("clientIdA"));
        assertTrue(filesAfterFirstDelete[0].contains("clientIdX"));

        repo.deletePlanConfig("serviceId1", "clientIdX");
        final String[] filesAfterSecondDelete = repo.dir(ConfigType.PLAN).toFile().list();
        assertTrue(ArrayUtils.isEmpty(filesAfterSecondDelete));
    }

    @Test
    public void testFindLatest() {
        OfflineConfigurationCacheRepo repo = createRepo();
        final String serviceId1 = "serviceId1";

        String latestConfig = null;
        for (int i = 0; i < NUMBER_ROTATING_FILES; i++) {
            final String config = createDummyConfig();
            if (config.equals(latestConfig)) {
                fail("Config has to change in every loop. Only this way it is possible to verify newest config was loaded");
            }
            repo.persistMappingRulesConfig(serviceId1, config);
            latestConfig = config;
        }

        assertEquals(latestConfig, repo.findMappingRulesConfig(serviceId1));
    }


    @Test
    public void testPersistWithRotation() {
        OfflineConfigurationCacheRepo repo = createRepo();
        final String serviceId1 = "serviceId1";

        // prepare all data - persist 10 times in order to get into the file rotation mode
        final List<String> writtenConfigs = new ArrayList<>();
        int filesWritten = 10;
        for (int i = 0; i < filesWritten; i++) {
            final String config = createDummyConfig();
            if (writtenConfigs.contains(config)) {
                fail("Config has to change in every loop. Only this way it is possible to verify newest config was loaded");
            }
            writtenConfigs.add(config);
            repo.persistMappingRulesConfig(serviceId1, config);
        }

        final Path configDirPath = dir.resolve(OfflineConfigurationCacheRepo.CONFIG_DIR_NAME).resolve(ConfigType.MAPPING_RULES.getId());
        final String[] files = configDirPath.toFile().list();
        assertNotNull(files);
        assertEquals(NUMBER_ROTATING_FILES, files.length);
        final String[] configsFromFiles = Arrays.stream(files).map(filename -> {
            try {
                return new String(Files.readAllBytes(configDirPath.resolve(filename)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // silent - will fail below when comparing list configs
                return "";
            }
        }).toArray(String[]::new);

        final List<String> lastWrittenConfigs = writtenConfigs.subList(NUMBER_ROTATING_FILES, writtenConfigs.size());
        assertEquals(filesWritten - NUMBER_ROTATING_FILES, lastWrittenConfigs.size());
        assertTrue(lastWrittenConfigs.containsAll(Arrays.asList(configsFromFiles)));
    }

    @Test
    public void testConfigBaseFileName() {
        assertEquals("mapping-rules_serviceA", configBaseFileName(ConfigType.MAPPING_RULES, new String[]{"serviceA"}));
        assertEquals("plan_serviceA_clientA", configBaseFileName(ConfigType.PLAN, new String[]{"serviceA", "clientA"}));
    }

    @Test
    public void testByBaseNameFilter() {
        Predicate<Path> pathPredicate = OfflineConfigurationCacheRepo.byBaseNameFilter("abc_serviceA");

        assertTrue(pathPredicate.test(Paths.get("abc_serviceA_20190322.json")));
        assertFalse(pathPredicate.test(Paths.get("abc_serviceB_20190322.json")));
    }

    @Test
    public void testDisabledMode() {
        OfflineConfigurationCacheRepo repo = new OfflineConfigurationCacheRepo(DISABLED);
        assertTrue("true, even when disabled", repo.persistPlanConfig("serviceId1", "clientIdA", "planconfig"));
        assertEquals(OfflineConfigurationState.DISABLED, repo.getState());
        final String[] files = dir.toFile().list();
        assertTrue(ArrayUtils.isEmpty(files));
    }

    @Test
    public void testEnabledMode() {
        OfflineConfigurationCacheRepo repo = new OfflineConfigurationCacheRepo(dir.toAbsolutePath().toString());
        assertEquals(OfflineConfigurationState.ENABLED, repo.getState());
        final String[] files = dir.toFile().list();
        assertFalse(ArrayUtils.isEmpty(files));
    }

    @Test
    public void testInvalidPath() {
        OfflineConfigurationCacheRepo repo = new OfflineConfigurationCacheRepo("/this/path/does/not/exist");
        assertEquals(OfflineConfigurationState.INVALID_CONFIG, repo.getState());
    }

    @Test
    public void testOidcPersist() {
        OfflineConfigurationCacheRepo repo = createRepo();
        String oidc = "{\"issuer\":\"https://sso-dev.sbb.ch/auth/realms/SBB_Public\",\"jwks_uri\":\"https://sso-dev.sbb.ch/auth/realms/SBB_Public/protocol/openid-connect/certs\"}";
        String tokenIssuerUrl = "https://sso-dev.sbb.ch//auth/realms/SBB_Public";
        assertTrue(repo.persistOidc(tokenIssuerUrl, oidc));
        assertEquals(oidc, repo.findOidc(tokenIssuerUrl));
    }

    @Test
    public void testJwksPersist() throws IOException {
        OfflineConfigurationCacheRepo repo = createRepo();
        String jwks = IOUtils.toString(OfflineConfigurationCacheRepoTest.class.getResourceAsStream("/jwks/rh-sso.json"), StandardCharsets.UTF_8);
        String jwksUrl = "https://sso-dev.sbb.ch/auth/realms/SBB_Public/protocol/openid-connect/certs";
        assertTrue(repo.persistJwks(jwksUrl, jwks));
        assertEquals(jwks, repo.findJwks(jwksUrl));
    }

    @Test
    public void urlToFileName() {
        assertEquals("", OfflineConfigurationCacheRepo.urlToFileName(null));
        assertEquals("", OfflineConfigurationCacheRepo.urlToFileName(""));
        assertEquals("sso-dev_sbb_ch_auth_realms_SBB_Public", OfflineConfigurationCacheRepo.urlToFileName("http://sso-dev.sbb.ch/auth/realms/SBB_Public"));
        assertEquals("sso-dev_sbb_ch_auth_realms_SBB_Public", OfflineConfigurationCacheRepo.urlToFileName("https://sso-dev.sbb.ch/auth/realms/SBB_Public"));
    }

}