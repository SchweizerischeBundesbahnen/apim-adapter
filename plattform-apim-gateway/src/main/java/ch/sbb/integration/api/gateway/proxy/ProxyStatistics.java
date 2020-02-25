package ch.sbb.integration.api.gateway.proxy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class ProxyStatistics {
    private static final ProxyStatistics INSTANCE = new ProxyStatistics();

    public static ProxyStatistics get() {
        return INSTANCE;
    }

    /**
     * Increments an AtomicInteger up to its max positive value. If reached, it restarts at 1
     * E.g. (2147483647 = Integer.MAX_VALUE)
     * <ul>
     * <li>0 -> 1</li>
     * <li>2147483646 -> 2147483647</li>
     * <li>2147483647 -> 1</li>
     * </ul>
     */
    private static final IntUnaryOperator INCREMENT_UNSIGNED_OVERFLOW = i -> i < Integer.MAX_VALUE ? i + 1 : 1;

    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger retries = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger queuedRequestFailed = new AtomicInteger();
    private final AtomicInteger unresolvableBackend = new AtomicInteger();

    private final AtomicInteger unresolvedAddressException = new AtomicInteger();

    public void incrementCompleted() {
        completed.updateAndGet(INCREMENT_UNSIGNED_OVERFLOW);
    }

    public void incrementRetries() {
        retries.updateAndGet(INCREMENT_UNSIGNED_OVERFLOW);
    }

    public void incrementFailed() {
        failed.updateAndGet(INCREMENT_UNSIGNED_OVERFLOW);
    }

    public void incrementQueuedRequestFailed() {
        queuedRequestFailed.updateAndGet(INCREMENT_UNSIGNED_OVERFLOW);
    }

    public void incrementUnresolvableBackend() {
        unresolvableBackend.updateAndGet(INCREMENT_UNSIGNED_OVERFLOW);
    }

    public void incrementUnresolvedAddressException() {
        unresolvedAddressException.updateAndGet(INCREMENT_UNSIGNED_OVERFLOW);
    }

    public int getCompleted() {
        return completed.get();
    }

    public int getRetries() {
        return retries.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public int getQueuedRequestFailed() {
        return queuedRequestFailed.get();
    }

    public int getUnresolvableBackend() {
        return unresolvableBackend.get();
    }

    public int getUnresolvedAddressException() {
        return unresolvedAddressException.get();
    }
}
