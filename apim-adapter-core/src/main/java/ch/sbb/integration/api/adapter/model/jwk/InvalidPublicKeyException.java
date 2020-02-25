package ch.sbb.integration.api.adapter.model.jwk;

public class InvalidPublicKeyException extends Exception {

    public InvalidPublicKeyException(String msg, Throwable cause) {
        super(msg, cause);
    }
}