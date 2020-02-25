package ch.sbb.integration.api.adapter.model;

import java.time.ZonedDateTime;
import java.util.Map;

public class TransactionsRequest {
    private final String appId;
    private final ZonedDateTime timestamp;
    private final Map<String, Long> usage;
    private final boolean active;

    public TransactionsRequest(String appId, Map<String, Long> usage) {
        this(appId, ZonedDateTime.now(), usage, true);
    }

    public TransactionsRequest(String appId, ZonedDateTime timestamp, Map<String, Long> usage) {
        this(appId, timestamp, usage, false);
    }

    private TransactionsRequest(String appId, ZonedDateTime timestamp, Map<String, Long> usage, boolean active) {
        this.appId = appId;
        this.timestamp = timestamp;
        this.usage = usage;
        this.active = active;
    }

    public String getAppId() {
        return appId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Long> getUsage() {
        return usage;
    }

    public boolean isActive() {
        return active;
    }
}
