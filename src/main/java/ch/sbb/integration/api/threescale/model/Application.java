package ch.sbb.integration.api.threescale.model;

import threescale.v3.api.AuthorizeResponse;

import java.io.Serializable;

/**
 * Created by u217269 on 16.02.2018.
 */
public class Application implements Serializable {

    private String clientId;
    private String applicationId;
    private AuthorizeResponse authorizeResponse;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public AuthorizeResponse getAuthorizeResponse() {
        return authorizeResponse;
    }

    public void setAuthorizeResponse(AuthorizeResponse authorizeResponse) {
        this.authorizeResponse = authorizeResponse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Application that = (Application) o;

        return clientId.equals(that.clientId);
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }

}
