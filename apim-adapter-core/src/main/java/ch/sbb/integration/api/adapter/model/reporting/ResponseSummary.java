package ch.sbb.integration.api.adapter.model.reporting;

import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

public class ResponseSummary {

    private final String clientId;
    private final int httpStatus;
    private final String metricSysName;
    private final ZonedDateTime minuteTimestamp;

    public ResponseSummary(String clientId, int httpStatus, String metricSysName) {
        Objects.requireNonNull(clientId, "clientId is null");
        Objects.requireNonNull(metricSysName, "metricSysName is null");

        this.clientId = clientId;
        this.httpStatus = httpStatus;
        this.metricSysName = metricSysName;
        minuteTimestamp = now()
                .with(SECOND_OF_MINUTE, 0)
                .with(MILLI_OF_SECOND,0);
    }

    public String getClientId() {
        return clientId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMetricSysName() {
        return metricSysName;
    }

    public ZonedDateTime getTimestamp() {
        return minuteTimestamp;
    }

    public boolean isWithin24Hours() {
        ZonedDateTime oneDayOld = ZonedDateTime.now().minusHours(24);
        return minuteTimestamp.isAfter(oneDayOld);
    }

    @Override
    public String toString() {
        return "ResponseSummary{" +
                "clientId='" + clientId + '\'' +
                ", httpStatus=" + httpStatus +
                ", metricSysName='" + metricSysName + '\'' +
                ", minuteTimestamp=" + minuteTimestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseSummary responseSummary = (ResponseSummary) o;
        return httpStatus == responseSummary.httpStatus &&
                clientId.equals(responseSummary.clientId) &&
                metricSysName.equals(responseSummary.metricSysName) &&
                minuteTimestamp.equals(responseSummary.minuteTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, httpStatus, metricSysName, minuteTimestamp);
    }
}
