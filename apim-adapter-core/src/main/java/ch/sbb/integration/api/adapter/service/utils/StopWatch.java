package ch.sbb.integration.api.adapter.service.utils;

/**
 * Simple stop watch so we don't need an additional dependency
 */
public class StopWatch {
    private long startTime;
    private long stopTime;

    public StopWatch start() {
        this.startTime = System.currentTimeMillis();
        return this;
    }

    public StopWatch stop() {
        this.stopTime = System.currentTimeMillis();
        return this;
    }

    public long getMillis() {
        return (stopTime - startTime);
    }
}
