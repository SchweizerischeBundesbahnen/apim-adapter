package ch.sbb.integration.api.adapter.service.cache;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.model.OAuthToken;
import ch.sbb.integration.api.adapter.model.OAuthToken.TokenStateEnum;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuer;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuerStore;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TokenToParsedTokenCache implements Cache<OAuthToken> {

    private static final Logger LOG = LoggerFactory.getLogger(TokenToParsedTokenCache.class);
    private static final int MAXIMUM_CACHE_SIZE = 10_000;
    private static final int MINIMAL_TOKEN_LIFETIME = 30;

    private LoadingCache<String, OAuthToken> tokenToClientId;
    private final TokenIssuerStore tokenIssuerStore;

    public TokenToParsedTokenCache(ApimAdapterConfig config, RestConfig restConfig, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
        this.tokenIssuerStore = TokenIssuerStore.init(config, restConfig, offlineConfigurationCacheRepo);
        initCache(config);
    }

    @Override
    public OAuthToken get(String token) {
        if (token == null) {
            return null;
        }

        return tokenToClientId.get(token);
    }

    @Override
    public long size() {
        return tokenToClientId.estimatedSize();
    }

    @Override
    public com.github.benmanes.caffeine.cache.Cache<?, ?> get() {
        return tokenToClientId;
    }

    private void initCache(ApimAdapterConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (config.isMonitoringNotDisabledOrMinimal()) {
            builder.recordStats();
        }

        tokenToClientId = builder
                .maximumSize(MAXIMUM_CACHE_SIZE)
                .expireAfterWrite(MINIMAL_TOKEN_LIFETIME, TimeUnit.MINUTES)
                .build(this::parseToken);
    }

    public OAuthToken parseToken(String token) {
        TokenIssuer tokenIssuer = null;
        String clientId = null;
        String type = null;
        Long expiration = null;
        try {
            final DecodedJWT jwt = JWT.decode(token);
            final String issuer = jwt.getIssuer();
            // claim clientid is only in tokens available, which are issued with client credential flow
            // use azp instead (Authorized party --> the party to which this token was issued)
            final Claim azpClaim = jwt.getClaim("azp");
            if (azpClaim != null) {
                clientId = azpClaim.asString();
            } else {
                LOG.warn(ReasonCode.APIM_2041.pattern(), token);
            }
            final Claim expirationClaim = jwt.getClaim("exp");
            final Claim typeClaim = jwt.getClaim("typ");
            type = typeClaim != null ? typeClaim.asString() : null;
            expiration = expirationClaim != null ? expirationClaim.asLong() : null;

            final Optional<TokenIssuer> optionalTokenIssuer = tokenIssuerStore.resolve(issuer);
            if (optionalTokenIssuer.isPresent()) {
                tokenIssuer = optionalTokenIssuer.get();
                tokenIssuer.getVerifier().verify(jwt);

                return new OAuthToken(TokenStateEnum.VALID, tokenIssuer, token, clientId, expiration, type);
            } else {
                return new OAuthToken(TokenStateEnum.UNKNOWN_TOKEN_ISSUER, tokenIssuer, token, clientId, expiration, type);
            }
        } catch (SignatureVerificationException e) {
            LOG.info(ReasonCode.APIM_1010.pattern());
            LOG.debug("Exception for failed signature is: ", e);
            return new OAuthToken(TokenStateEnum.INVALID_SIGNATURE, tokenIssuer, token, clientId, expiration, type);
        } catch (JWTVerificationException e) {
            LOG.debug("Unable to parse Token token='{}'", token, e);
            return new OAuthToken(TokenStateEnum.INVALID, tokenIssuer, token, clientId, expiration, type);
        } catch (Exception e) {
            LOG.error(ReasonCode.APIM_3023.pattern(), e);
            return null;
        }
    }

    public void invalidateCache() {
        LOG.info(ReasonCode.APIM_1006.pattern(), this.get().estimatedSize());
        this.get().invalidateAll();
    }
}
