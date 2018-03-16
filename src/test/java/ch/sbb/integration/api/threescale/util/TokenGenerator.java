package ch.sbb.integration.api.threescale.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Date;

/**
 * Created by u217269 on 01.03.2018.
 */
public class TokenGenerator {

    public static String generateValidToken(String clientId, long ttlSeconds) {

        try {
            Algorithm algorithmHS = Algorithm.HMAC256("junit-secret");

            Date expiration = Date.from(Instant.now().plusSeconds(ttlSeconds));

            return JWT.create()
                    .withSubject("f754d990-3fe1-4112-95fb-1ca262857e25")
                    .withIssuer("https://sso.sbb.ch/auth/realms/SBB_Public")
                    .withExpiresAt(expiration)
                    .withClaim("typ", "Bearer")
                    .withClaim("azp", clientId)
                    .withClaim("clientId", clientId)
                    .sign(algorithmHS);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Tests must fail here
        }

    }

}
