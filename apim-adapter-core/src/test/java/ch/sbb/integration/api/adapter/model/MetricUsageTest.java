package ch.sbb.integration.api.adapter.model;

import ch.sbb.integration.api.adapter.model.usage.MetricUsage;
import org.junit.Test;

import java.time.format.DateTimeFormatter;

import static java.time.ZonedDateTime.now;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MetricUsageTest {

    @Test
    public void calculateCurrentPeriod() {
        //Arrange
        MetricUsage metricUsage = MetricUsage.limitedMetric("CLIENT_ID",
                "METRIC_NAME",
                0L,
                0L,
                now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")),
                now().minusDays(1).minusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")));
        // Act
        MetricUsage currentPeriod = MetricUsage.getCurrentPeriod(metricUsage);

        // assert
        assertThat(currentPeriod.isLimited(), is(true));
        assertThat(MetricUsage.getCurrentPeriod(currentPeriod), is(currentPeriod));
        assertThat(currentPeriod.getMetricSysName(), is("METRIC_NAME"));
    }
}