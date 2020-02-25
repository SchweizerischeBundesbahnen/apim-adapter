package ch.sbb.integration.api.adapter.model.tokenissuer;

import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.model.jwk.InvalidPublicKeyException;
import ch.sbb.integration.api.adapter.model.jwk.Jwk;
import ch.sbb.integration.api.adapter.model.jwk.Jwks;
import ch.sbb.integration.api.adapter.service.exception.TokenIssuerException;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TokenIssuer {
    private static final Logger LOG = LoggerFactory.getLogger(TokenIssuer.class);

    private final OfflineConfigurationCacheRepo offlineConfigurationCacheRepo;
    private final ResteasyClient resteasyClient;

    // e.g. https://sso-dev.sbb.ch
    private String host;

    // e.g. https://sso-dev.sbb.ch/auth/realms/SBB_Public
    private String issuerUrl;

    // e.g. SBB_Public
    private String realm;

    // https://sso-dev.sbb.ch/auth/realms/SBB_Public/.well-known/openid-configuration
    private String oidcUrl;

    // "jwks_uri" from the "openid-configuration"
    // e.g. "https://sso-dev.sbb.ch/auth/realms/SBB_Public/protocol/openid-connect/certs"
    private String jwksUri;

    private Jwks jwks;
    private LocalDateTime jwksReloadTimestamp;

    private final JWTVerifier verifier = createVerifier();

    public TokenIssuer(ResteasyClient resteasyClient, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
        this.resteasyClient = resteasyClient;
        this.offlineConfigurationCacheRepo = offlineConfigurationCacheRepo;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
    }

    public String getIssuerUrl() {
        return issuerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getOidcUrl() {
        return oidcUrl;
    }

    public void setOidcUrl(String oidcUrl) {
        this.oidcUrl = oidcUrl;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Jwks getJwks() {
        return jwks;
    }

    public void setJwks(Jwks jwks) {
        this.jwks = jwks;
    }

    public boolean hasKey(String keyId) {
        return getKey(keyId).isPresent();
    }

    Optional<Jwk> getKey(String keyId) {
        return getJwks().getKey(keyId);
    }

    public JWTVerifier getVerifier() {
        return verifier;
    }

    /**
     * @return true, if JWKS has been reloaded and the JWKS keys have changed, false otherwise
     */
    public boolean reloadJwks() {

        if (jwksReloadTimestamp == null || LocalDateTime.now().minusMinutes(1).isAfter(jwksReloadTimestamp)) {
            LOG.debug("Reloading JWKS, at least one minute has passed since last try");
            try {
                final Jwks newJwks = loadJwks(false);

                if (newJwks.getKeys().values().isEmpty()) {
                    LOG.error(ReasonCode.APIM_3022.pattern(), jwksUri);
                    return false;
                } else {
                    final Set<String> existingKeyIds = this.jwks.getKeyIds();
                    final Set<String> newKeyIds = newJwks.getKeyIds();

                    final Set<String> removedKeyIds = new HashSet<>(existingKeyIds);
                    removedKeyIds.removeAll(newKeyIds);
                    removedKeyIds.forEach(keyId -> LOG.warn(ReasonCode.APIM_2039.pattern(), keyId, jwksUri));

                    final Set<String> addedKeyIds = new HashSet<>(newKeyIds);
                    addedKeyIds.removeAll(existingKeyIds);
                    addedKeyIds.forEach(keyId -> LOG.info(ReasonCode.APIM_1011.pattern(), keyId, jwksUri));

                    // swapping the JWKS
                    this.jwks = newJwks;

                    jwksReloadTimestamp = LocalDateTime.now();
                    return !addedKeyIds.isEmpty() || !existingKeyIds.isEmpty();
                }
            } catch (TokenIssuerException e) {
                LOG.debug("Error during reloading of JWKS. Pause for at least one minute before next attempt", e);
                jwksReloadTimestamp = LocalDateTime.now();
                return false;
            } catch (Exception e) {
                LOG.warn(ReasonCode.APIM_2040.pattern(), e);
                jwksReloadTimestamp = LocalDateTime.now();
                return false;
            }
        } else {
            LOG.debug("Not reloading JWKS as last attempt was within last minute");
            return false;
        }
    }

    void initJwks() {
        setJwksUri(loadJwksUriFromOidc());
        setJwks(loadJwks(true));
    }

    private String loadJwksUriFromOidc() {
        final StopWatch sw = new StopWatch().start();
        try (Response response = resteasyClient.target(oidcUrl).request().get()) {
            LOG.debug("OIDC loaded in {} ms OidcUrl{}", sw.stop().getMillis(), oidcUrl);
            if (response.getStatus() == 200) {
                final String oidcJson = response.readEntity(String.class);
                offlineConfigurationCacheRepo.persistOidc(issuerUrl, oidcJson);
                return parseJwksUri(oidcJson);
            } else {
                LOG.warn(ReasonCode.APIM_2038.pattern(), oidcUrl, sw.stop().getMillis(), response.getStatus());
                return parseJwksUri(offlineConfigurationCacheRepo.findOidc(issuerUrl));
            }
        } catch (Exception e) {
            LOG.warn(ReasonCode.APIM_2038.pattern(), oidcUrl, sw.stop().getMillis(), -1);
            try {
                return parseJwksUri(offlineConfigurationCacheRepo.findOidc(issuerUrl));
            } catch (IOException e1) {
                return null;
            }
        }
    }

    private String parseJwksUri(String oidcJson) throws IOException {
        final JsonNode jsonNode = new ObjectMapper().readTree(oidcJson);
        return jsonNode.get("jwks_uri").textValue();
    }

    public Jwks loadJwks(boolean loadFromOfflineCache) {
        final StopWatch sw = new StopWatch().start();
        try (Response response = resteasyClient.target(jwksUri).request().get()) {
            LOG.info(ReasonCode.APIM_1034.pattern(), sw.stop().getMillis(), jwksUri);
            if (response.getStatus() == 200) {
                final String jwksJson = response.readEntity(String.class);
                offlineConfigurationCacheRepo.persistJwks(jwksUri, jwksJson);
                final JsonNode jsonNode = new ObjectMapper().readTree(jwksJson);
                return new Jwks(jsonNode.toString());
            } else {
                final String msg = ReasonCode.APIM_2024.format(jwksUri, sw.stop().getMillis(), response.getStatus());
                if (loadFromOfflineCache) {
                    LOG.warn(ReasonCode.APIM_2042.pattern(), msg);
                    return toJwks(offlineConfigurationCacheRepo.findJwks(jwksUri))
                            .orElseThrow(() -> new TokenIssuerException(msg));
                } else {
                    throw new TokenIssuerException(msg);
                }
            }
        } catch (Exception e) {
            try {
                if (loadFromOfflineCache) {
                    final String msg = ReasonCode.APIM_2024.format(jwksUri, sw.stop().getMillis(), -1);
                    LOG.warn(msg, e);
                    return toJwks(offlineConfigurationCacheRepo.findJwks(jwksUri))
                            .orElseThrow(() -> e);
                } else {
                    throw e;
                }
            } catch (Exception ex) {
                throw new TokenIssuerException("Unable to load JWKS", ex);
            }
        }
    }

    private Optional<Jwks> toJwks(String jwksJson) throws IOException {
        if (jwksJson != null && !jwksJson.isEmpty()) {
            return Optional.of(new Jwks(jwksJson));
        } else {
            return Optional.empty();
        }
    }

    private JWTVerifier createVerifier() {
        LOG.debug("Generating new Verifier for token issuerUrl={}", issuerUrl);

        final Algorithm alg = Algorithm.RSA256(new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String keyId) {
                final Optional<Jwk> optionalJwk = getKey(keyId);
                if (optionalJwk.isPresent()) {
                    try {
                        return optionalJwk.get().getRsaPublicKey();
                    } catch (InvalidPublicKeyException e) {
                        LOG.warn(ReasonCode.APIM_2036.pattern(), keyId, jwksUri, e);
                    }
                } else {
                    LOG.warn(ReasonCode.APIM_2037.pattern(), keyId, jwksUri);
                }
                return null;
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                // not needed in order to validate signature
                return null;
            }

            @Override
            public String getPrivateKeyId() {
                // not needed in order to validate signature
                return null;
            }
        });

        return JWT.require(alg).build();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TokenIssuer{");
        sb.append("host='").append(host).append('\'');
        sb.append(", issuerUrl='").append(issuerUrl).append('\'');
        sb.append(", realm='").append(realm).append('\'');
        sb.append(", oidcUrl='").append(oidcUrl).append('\'');
        sb.append(", jwksUri='").append(jwksUri).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public void resetJwksReloadTimestamp() {
        LOG.debug("Resetting JWKS reload timestamp");
        jwksReloadTimestamp = null;
    }
}
