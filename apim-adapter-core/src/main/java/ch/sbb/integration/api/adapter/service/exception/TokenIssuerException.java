package ch.sbb.integration.api.adapter.service.exception;

public class TokenIssuerException extends RuntimeException {

    public TokenIssuerException(String message) {
        super(message);
    }

    public TokenIssuerException(String message, Throwable t) {
        super(message, t);
    }
}
