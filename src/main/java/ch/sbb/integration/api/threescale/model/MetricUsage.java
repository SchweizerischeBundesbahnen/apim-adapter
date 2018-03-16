package ch.sbb.integration.api.threescale.model;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by u217269 on 16.02.2018.
 */
public class MetricUsage implements Serializable {

    private String clientId;
    private String metricName;
    private AtomicLong periodStartMillis;
    private AtomicLong periodEndMillis;

    private final AtomicLong limit;
    private final AtomicLong base;
    private final AtomicLong countSinceLastSync; // for statistics
    private final AtomicLong countInCurrentPeriod; // for rate-limiting
    private final boolean isLimited;

    public static MetricUsage unlimitedMetric(String clientId, String metricName) {
        return new MetricUsage(false, clientId, metricName, null, null, null, null);
    }

    public static MetricUsage limitedMetric(String clientId, String metricName, Long limit, Long base, String periodStartDateTime, String periodEndDateTime) {
        return new MetricUsage(true, clientId, metricName, limit, base, periodStartDateTime, periodEndDateTime);
    }

    private MetricUsage(boolean isLimited, String clientId, String metricName, Long limit, Long base, String periodStartDateTime, String periodEndDateTime) {
        this.clientId = clientId;
        this.metricName = metricName;

        this.countSinceLastSync = new AtomicLong();
        this.countInCurrentPeriod = new AtomicLong();

        this.isLimited = isLimited;

        if (this.isLimited) {
            this.limit = new AtomicLong(limit);
            this.base = new AtomicLong(base);
            this.periodStartMillis = toZonedDateTimestampInMillis(periodStartDateTime);
            this.periodEndMillis = toZonedDateTimestampInMillis(periodEndDateTime);
        } else {
            this.limit = null;
            this.base = null;
            this.periodStartMillis = null;
            this.periodEndMillis = null;
        }

    }

    public String getMetricName() {
        return metricName;
    }

    public AtomicLong getBase() {
        return base;
    }

    public AtomicLong getCountSinceLastSync() {
        return countSinceLastSync;
    }

    public AtomicLong getCountInCurrentPeriod() {
        return countInCurrentPeriod;
    }

    public boolean hitMetric() {
        countSinceLastSync.incrementAndGet();

        if (isLimited) {
            if (System.currentTimeMillis() > periodEndMillis.get()) {
                switchToNextPeriod();
            }

            long currentUsage = countInCurrentPeriod.incrementAndGet();
            return limit.get() >= 0 && base.get() + currentUsage <= limit.get();
        } else {
            return true;
        }
    }

    public void synchronizePeriod(long base, long limit, String periodStart, String periodEnd) {
        if (isLimited) { // synchronization only makes sense for limited Metrics
            this.base.set(base);
            this.limit.set(limit);
            this.periodStartMillis = toZonedDateTimestampInMillis(periodStart);
            this.periodEndMillis = toZonedDateTimestampInMillis(periodEnd);
        }
    }

    public long resetAndGetCounter() {
        long usage = countSinceLastSync.get();
        countSinceLastSync.set(0);
        return usage;
    }

    /**
     * This method is being used to "roll-back" after calling {@link #resetAndGetCounter()}
     */
    public void addHitCount(long hits) {
        countSinceLastSync.addAndGet(hits);
    }

    private synchronized void switchToNextPeriod() {
        base.set(0);
        countInCurrentPeriod.set(0);

        long periodLength = periodEndMillis.get() + periodStartMillis.get();
        periodStartMillis.addAndGet(periodLength);
        periodEndMillis.addAndGet(periodLength);
    }

    private AtomicLong toZonedDateTimestampInMillis(String periodStartDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
        long milliseconds = ZonedDateTime
                .parse(periodStartDateTime, formatter)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return new AtomicLong(milliseconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricUsage that = (MetricUsage) o;

        return clientId.equals(that.clientId) && metricName.equals(that.metricName);
    }

    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + metricName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MetricUsage{" +
                "clientId='" + String.valueOf(clientId) + '\'' +
                ", isLimited='" + String.valueOf(isLimited) + '\'' +
                ", metricName='" + String.valueOf(metricName) + '\'' +
                ", periodStartMillis=" + String.valueOf(periodStartMillis) +
                ", periodEndMillis=" + String.valueOf(periodEndMillis) +
                ", limit=" + String.valueOf(limit) +
                ", base=" + String.valueOf(base) +
                ", countSinceLastSync=" + String.valueOf(countSinceLastSync) +
                ", countInCurrentPeriod=" + String.valueOf(countInCurrentPeriod) +
                '}';
    }

}
