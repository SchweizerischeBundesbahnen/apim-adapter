package ch.sbb.integration.api.adapter.service.exception;

/**
 * Created by u217269 on 16.02.2018.
 */
public class ThreeScaleAdapterException extends RuntimeException {

    public ThreeScaleAdapterException(String message) {
        super(message);
    }

    public ThreeScaleAdapterException(String message, Throwable t) {
        super(message, t);
    }
}
