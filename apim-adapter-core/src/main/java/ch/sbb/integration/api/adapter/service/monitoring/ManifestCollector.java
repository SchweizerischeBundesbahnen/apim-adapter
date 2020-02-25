package ch.sbb.integration.api.adapter.service.monitoring;

import ch.sbb.integration.api.adapter.config.ReasonCode;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManifestCollector extends Collector {
    private static final Logger LOG = LoggerFactory.getLogger(ManifestCollector.class);

    private final List<MetricFamilySamples> mfs = new ArrayList<>();
    private final String manifestImplementationName;
    private final String manifestImplementationNameEscaped;

    public ManifestCollector(ManifestExtractor extractor, String manifestImplementationName) {
        this.manifestImplementationName = manifestImplementationName;
        this.manifestImplementationNameEscaped = escapePrometheusText(manifestImplementationName);
        Map<String, String> apimAdapterManifest = extractor.extractFromManifest(this.manifestImplementationName);
        if(!apimAdapterManifest.isEmpty()) {
            addMetric(apimAdapterManifest);
        }
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return mfs;
    }

    private void addMetric(Map<String, String> manifest) {
        LOG.debug("initializing manifest collector for {}", manifestImplementationName);

        //Create a list here, because we depende on the order of the values.
        List<Map.Entry<String, String>> manifestList = new ArrayList<>(manifest.entrySet()) ;

        LOG.info(ReasonCode.APIM_1029.pattern(), manifestList);

        List<String> keys =  manifestList.stream()
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .map(this::escapePrometheusText)
                .collect(Collectors.toList());

        List<String> values = manifestList.stream()
                .map(Map.Entry::getValue)
                .map(Object::toString)
                .collect(Collectors.toList());

        GaugeMetricFamily gatewayConfig = new GaugeMetricFamily(
                manifestImplementationNameEscaped + "_manifest",
                "manifest of " + manifestImplementationNameEscaped,
                keys);
        gatewayConfig.addMetric(
                values,
                1L);
        mfs.add(gatewayConfig);
    }


    private String escapePrometheusText(String prometheusText) {
        return prometheusText.replace("-","_");
    }
}
