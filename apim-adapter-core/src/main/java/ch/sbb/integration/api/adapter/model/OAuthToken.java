package ch.sbb.integration.api.adapter.model;

import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuer;

/**
 * Created by u217269 on 28.02.2018.
 */
public class OAuthToken {
    private final TokenStateEnum tokenState;
    private final TokenIssuer tokenIssuer;
    private final String clientId;
    private final String token;
    private final Long expiration;
    private final String type;
    private final boolean bearer;

    public OAuthToken(TokenStateEnum tokenState, TokenIssuer tokenIssuer, String token, String clientId, Long expirationSeconds, String type) {
        this.tokenState = tokenState != null ? tokenState : TokenStateEnum.INVALID;
        this.tokenIssuer = tokenIssuer;
        this.clientId = clientId;
        this.token = token;
        this.expiration = expirationSeconds == null ? null : expirationSeconds * 1000; // convert to millis
        this.type = type;
        this.bearer = "Bearer".equalsIgnoreCase(type);
    }

    public TokenIssuer getTokenIssuer() {
        return tokenIssuer;
    }

    public String getClientId() {
        return clientId;
    }

    public String getType() {
        return type;
    }

    public boolean isValid() {
        return tokenState == TokenStateEnum.VALID &&
                tokenIssuer != null &&
                clientId != null &&
                bearer &&
                !isExpired();
    }

    public boolean isExpired() {
        return expiration == null || System.currentTimeMillis() > expiration;
    }

    public String getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OAuthToken that = (OAuthToken) o;

        return token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return "OAuthToken{" +
                "tokenState='" + tokenState + '\'' +
                ", tokenIssuer='" + tokenIssuer + '\'' +
                ", clientId='" + clientId + '\'' +
                ", token='" + token + '\'' +
                ", expiration=" + expiration +
                '}';
    }

    public Long getExpiration() {
        return expiration;
    }

    public boolean isBearer() {
        return bearer;
    }

    public TokenStateEnum getTokenState() {
        if (tokenState == TokenStateEnum.VALID && isExpired()) {
            return TokenStateEnum.EXPIRED;
        }
        return tokenState;
    }

    public enum TokenStateEnum {
        VALID,
        EXPIRED,
        UNKNOWN_TOKEN_ISSUER,
        INVALID_SIGNATURE,
        INVALID
    }
}
