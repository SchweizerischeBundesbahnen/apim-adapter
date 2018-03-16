package ch.sbb.integration.api.threescale.service.exception;

/**
 * Created by u217269 on 16.02.2018.
 */
public class ThreeScaleAdapterException extends RuntimeException {

    public ThreeScaleAdapterException(String message) {
        super(message);
    }

    public ThreeScaleAdapterException(String message, Exception e) {
        super(message, e);
    }
}
