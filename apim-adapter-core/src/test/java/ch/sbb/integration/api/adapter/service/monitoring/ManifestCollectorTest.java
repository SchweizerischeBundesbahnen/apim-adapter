package ch.sbb.integration.api.adapter.service.monitoring;

import com.google.common.collect.ImmutableMap;
import io.prometheus.client.Collector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ManifestCollectorTest {

    @Mock
    private ManifestExtractor extractor;

    @Test
    public void testMetricOnPopulatedMap() {
        //arrange
        String key1 = "manifest-key-1";
        String val1 = "manifest-value-1";
        String key2 = "manifest-key-2";
        String val2 = "manifest-value-2";
        Map<String, String> manifest = ImmutableMap.of(key1, val1, key2, val2);
        Mockito.when(extractor.extractFromManifest(any())).thenReturn(manifest);

        //Act
        ManifestCollector testee = new ManifestCollector(extractor, "apim-adapter");

        //Assert
        List<Collector.MetricFamilySamples> collect = testee.collect();

        Collector.MetricFamilySamples.Sample sample = collect.get(0).samples.get(0);
        assertThat(sample.labelNames, hasItem(key1.replaceAll("-","_")));
        assertThat(sample.labelNames, hasItem(key2.replaceAll("-","_")));
        assertThat(sample.labelValues, hasItem(val1));
        assertThat(sample.labelValues, hasItem(val2));
    }

    @Test
    public void testMetricOnEmptyMap() {
        //arrange
        Map<String, String> manifest = Collections.emptyMap();
        Mockito.when(extractor.extractFromManifest(any())).thenReturn(manifest);

        //Act
        ManifestCollector testee = new ManifestCollector(extractor, "apim-adapter");

        //Assert
        List<Collector.MetricFamilySamples> collect = testee.collect();
        assertThat(collect.size(), is(0));
    }
}