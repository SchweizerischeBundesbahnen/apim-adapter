package ch.sbb.integration.api.threescale.model;

import java.io.Serializable;

/**
 * Created by u217269 on 28.02.2018.
 */
public class OAuthToken implements Serializable {

    private final String clientId;
    private final String token;
    private final Long expiration;
    private final String type;

    public OAuthToken(String token, String clientId, long expiration, String type) {
        this.clientId = clientId;
        this.token = token;
        this.expiration = expiration * 1000; // convert to millis
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public String getType() {
        return type;
    }

    public boolean isValid() {
        return clientId != null && !isExpired();
    }

    public boolean isExpired() {
        return expiration == null || System.currentTimeMillis() > expiration;
    }

    public String getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

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
                "clientId='" + clientId + '\'' +
                ", token='" + token + '\'' +
                ", expiration=" + expiration +
                '}';
    }

    public Long getExpiration() {
        return expiration;
    }
}
