package ch.sbb.integration.api.adapter.model.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1003;

/**
 * Created by u217269 on 16.02.2018.
 */
public final class MetricUsage implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(MetricUsage.class);
    private static final long serialVersionUID = -6942725462035978945L;

    private final String clientId;
    private final String metricSysName;
    private final ZonedDateTime periodStart;
    private final ZonedDateTime periodEnd;

    private final boolean limited;
    private final Long limit;
    private final Long base;
    private final AtomicLong currentUsage;

    private MetricUsage(String clientId,
                        String metricSysName,
                        ZonedDateTime periodStartDateTime,
                        ZonedDateTime periodEndDateTime,
                        boolean limited,
                        Long limit,
                        Long base) {
        this.clientId = clientId;
        this.metricSysName = metricSysName;

        //Ensure that start < end
        if(periodStartDateTime.isBefore(periodEndDateTime)) {
            this.periodStart = periodStartDateTime;
            this.periodEnd = periodEndDateTime;
        } else {
            if(periodStartDateTime.isAfter(periodEndDateTime)) {
                this.periodStart = periodEndDateTime;
                this.periodEnd = periodStartDateTime;
            } else {
                //They are the same, we add a second to the end
                this.periodStart = periodStartDateTime;
                this.periodEnd = periodStartDateTime.plusSeconds(1);
            }
        }

        this.currentUsage = new AtomicLong();
        this.limited = limited;


        this.limit = limit;
        this.base = base;
    }

    public static MetricUsage unlimitedMetric(String clientId, String metricSysName) {
        return new MetricUsage(clientId, metricSysName, LocalDateTime.MIN.atZone(ZoneId.systemDefault()), LocalDateTime.MAX.atZone(ZoneId.systemDefault()), false, null, null);
    }

    public static MetricUsage limitedMetric(String clientId, String metricSysName, Long limit, Long base, String periodStartDateTime, String periodEndDateTime) {
        return new MetricUsage(clientId, metricSysName, toZonedDateTime(periodStartDateTime), toZonedDateTime(periodEndDateTime), true, limit, base);
    }

    public boolean isLimited() {
        return limited;
    }

    public String getMetricSysName() {
        return metricSysName;
    }

    public Long getBase() {
        return base;
    }

    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }

    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }

    public Long getLimit() {
        return limit;
    }

    public AtomicLong getCurrentUsage() {
        return currentUsage;
    }

    /**
     * @return True if metric is successful hit, false if current usage is exceeded
     */
    public boolean incrementCurrentUsage() {
        if (isLimited()) {
            long currentUsageIncremented = currentUsage.incrementAndGet();
            return (base + currentUsageIncremented) <= limit;
        } else {
            return true;
        }
    }

    private boolean isInPeriod() {
        return !isLimited() || !ZonedDateTime.now().isAfter(periodEnd);
    }

    private static ZonedDateTime toZonedDateTime(String periodStartDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
        return ZonedDateTime.parse(periodStartDateTime, formatter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricUsage that = (MetricUsage) o;
        return clientId.equals(that.clientId) &&
                metricSysName.equals(that.metricSysName) &&
                periodStart.equals(that.periodStart) &&
                periodEnd.equals(that.periodEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, metricSysName, periodStart, periodEnd);
    }

    @Override
    public String toString() {
        return "MetricUsage{" +
                "clientId='" + clientId + '\'' +
                ", metricSysName='" + metricSysName + '\'' +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", limited=" + limited +
                ", limit=" + limit +
                ", base=" + base +
                ", currentUsage=" + currentUsage +
                '}';
    }

    public static MetricUsage getCurrentPeriod(MetricUsage metricUsage) {
        MetricUsage currentMetricUsage = metricUsage;

        while(!currentMetricUsage.isInPeriod()){
            currentMetricUsage = getNextPeriod(currentMetricUsage);
        }
        return currentMetricUsage;
    }

    public static MetricUsage synchronizePeriods(MetricUsage master, MetricUsage slave) {
        LOG.debug("synchronizePeriod of metricSysName='{}' from clientId='{}'", master.metricSysName, master.clientId);

        if (!master.isLimited()) {
            return unlimitedMetric(master.clientId, master.metricSysName);
        }
        return new MetricUsage(master.clientId,
                master.metricSysName,
                master.periodStart,
                master.periodEnd,
                true,
                master.limit,
                master.base);
    }

    private static MetricUsage getNextPeriod(MetricUsage metricUsage) {
        Duration between = Duration.between(metricUsage.periodStart, metricUsage.periodEnd);
        ZonedDateTime nextPeriodStart = metricUsage.periodStart.plus(between);
        ZonedDateTime nextPeriodEnd = metricUsage.periodEnd.plus(between);
        LOG.info(APIM_1003.pattern(), metricUsage.metricSysName, metricUsage.clientId, nextPeriodStart, nextPeriodEnd);
        return new MetricUsage(metricUsage.clientId, metricUsage.metricSysName, nextPeriodStart, nextPeriodEnd, metricUsage.limited, metricUsage.limit, 0L);

    }
}
