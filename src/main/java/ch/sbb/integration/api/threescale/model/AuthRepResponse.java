package ch.sbb.integration.api.threescale.model;

import java.io.Serializable;

/**
 * Created by u217269 on 19.02.2018.
 */
public class AuthRepResponse implements Serializable {

    private final String clientId;
    private final boolean allowed;
    private final int httpStatus;
    private final String message;

    public AuthRepResponse(String clientId, boolean allowed, int httpStatus, String message) {
        this.clientId = clientId;
        this.allowed = allowed;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "AuthRepResponse{" +
                "allowed=" + allowed +
                ", httpStatus=" + httpStatus +
                ", message='" + message + '\'' +
                '}';
    }

}
