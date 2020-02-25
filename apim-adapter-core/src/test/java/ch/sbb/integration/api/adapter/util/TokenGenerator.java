package ch.sbb.integration.api.adapter.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Created by u217269 on 01.03.2018.
 */
public class TokenGenerator {
    public String generateBearerToken(String clientId, long ttlSeconds) {
        return generateToken(clientId, ttlSeconds, "Bearer");
    }

    public String generateToken(String clientId, long ttlSeconds, String type) {
        final TokenIssuerKeys.Key key = TokenIssuerKeys.getKey(0);
        final Algorithm algorithm = Algorithm.RSA256(key.getPublicKey(), key.getPrivateKey());
        final Date expiration = Date.from(Instant.now().plusSeconds(ttlSeconds));

        return JWT.create()
                .withIssuer("http://localhost:8099/auth/realms/SBB_Public")
                .withExpiresAt(expiration)
                .withClaim("typ", type)
                .withClaim("azp", clientId)
                .withClaim("clientId", clientId)
                .withKeyId(key.getKeyId())
                .sign(algorithm);

    }

    public String generateTokenWithRandomNewKey(String clientId, long ttlSeconds, boolean setKeyId) {
        final TokenIssuerKeys.Key key = TokenIssuerKeys.createKey();
        final Algorithm algorithm = Algorithm.RSA256(key.getPublicKey(), key.getPrivateKey());
        final Date expiration = Date.from(Instant.now().plusSeconds(ttlSeconds));

        JWTCreator.Builder builder = JWT.create()
                .withIssuer("http://localhost:8099/auth/realms/SBB_Public")
                .withExpiresAt(expiration)
                .withClaim("typ", "Bearer")
                .withClaim("azp", clientId)
                .withClaim("clientId", clientId);
        if (setKeyId) {
            builder.withKeyId(key.getKeyId());
        }
        return builder.sign(algorithm);
    }


    public String generateTokenWithNewKey(String clientId, long ttlSeconds) {
        final TokenIssuerKeys.Key key = TokenIssuerKeys.getKey(2);
        final Algorithm algorithm = Algorithm.RSA256(key.getPublicKey(), key.getPrivateKey());
        final Date expiration = Date.from(Instant.now().plusSeconds(ttlSeconds));

        return JWT.create()
                .withIssuer("http://localhost:8099/auth/realms/SBB_Public")
                .withExpiresAt(expiration)
                .withClaim("typ", "Bearer")
                .withClaim("azp", clientId)
                .withClaim("clientId", clientId)
                .withKeyId(key.getKeyId())
                .sign(algorithm);

    }

    public static synchronized TokenGenerator getInstance() {
        return new TokenGenerator();
    }

    public String getPublicKeyString() {
        return Base64.getEncoder().encodeToString(TokenIssuerKeys.getKey(0).getPublicKey().getEncoded());
    }

    public String getNewPublicKeyString() {
        return Base64.getEncoder().encodeToString(TokenIssuerKeys.getKey(1).getPublicKey().getEncoded());
    }

}
