package ch.sbb.integration.api.threescale.service.cache;

import ch.sbb.integration.api.threescale.model.OAuthToken;
import ch.sbb.integration.api.threescale.service.exception.ThreeScaleAdapterException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by u217269 on 22.02.2018.
 */
public class TokenToParsedTokenCache {

    private static final Logger LOG = Logger.getLogger(TokenToParsedTokenCache.class);

    private final LoadingCache<String, OAuthToken> tokenToClientId;

    private int minimalTokenLifetime = 30;

    public TokenToParsedTokenCache() {
        // --------------------------------------------------------------
        // Initialize Instance:
        // --------------------------------------------------------------
        tokenToClientId = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(minimalTokenLifetime, TimeUnit.MINUTES)
                .build(token -> extractClientId(token));
    }

    public OAuthToken get(String token) {
        return tokenToClientId.get(token);
    }

    public ConcurrentMap<String, OAuthToken> asMap() {
        return tokenToClientId.asMap();
    }

    public static OAuthToken extractClientId(String token) throws ThreeScaleAdapterException {

        DecodedJWT jwt = JWT.decode(token);
        Claim clientId = jwt.getClaim("clientId");
        Claim expiration = jwt.getClaim("exp");
        Claim type = jwt.getClaim("typ");

        if (clientId == null) {
            LOG.warn("Unable to parse ClientId from Token: " + token);
        }

        return new OAuthToken(
                token,
                clientId == null ? null : clientId.asString(),
                expiration == null ? null : expiration.asLong(),
                type == null ? null : type.asString());
    }

}
