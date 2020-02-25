package ch.sbb.integration.api.adapter.service.utils;

import static org.apache.http.HttpStatus.*;

/**
 * Created by u217269 on 27.02.2018.
 */
public enum ErrorReason {

    CLIENT_ID_HAS_NO_PERMISSION("Client has no permission", SC_FORBIDDEN),
    LIMIT_EXCEEDED("Limit exceeded", 429), // Http status code extension not included in standard http status
    PATH_NOT_FOUND("Path not found", SC_NOT_FOUND),
    METHOD_NOT_FOUND("Method not allowed", SC_METHOD_NOT_ALLOWED),
    BAD_REQUEST("Bad request", SC_BAD_REQUEST),
    UNAUTHORIZED("Authorization header is missing or invalid", SC_UNAUTHORIZED),
    EXPIRED_OR_INVALID("Token is expired or invalid", SC_UNAUTHORIZED),
    TOKEN_VALIDATION_FAILED("The validation of the Token failed, try again later ", SC_UNAUTHORIZED);


    private String message;

    private int httpStatus;


    private ErrorReason(String message, int httpStatus){
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
