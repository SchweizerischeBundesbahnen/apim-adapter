package ch.sbb.integration.api.adapter.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.Manifest;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2016;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2017;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2018;
import static java.util.stream.Collectors.toMap;

public class ManifestExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ManifestExtractor.class);

    private static final String IMPLEMENTATION_TITLE = "Implementation-Title";
    private static final List<String> MANIFEST_FIELDS = Arrays.asList("implementation-title", "implementation-version", "specification-title", "implementation-vendor-id");

    public Map<String, String> extractFromManifest(String manifestImplementationName) {
        Optional<Manifest> manifest = getManifest(manifestImplementationName);

        return manifest.map(this::filterManifestValues)
                .orElseGet(Collections::emptyMap);
    }

    private Map<String, String> filterManifestValues(Manifest m) {
        return m.getMainAttributes().entrySet()
                .stream()
                .filter(e -> manifestPredicate(e.getKey().toString()))
                .collect(toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()));
    }

    private boolean manifestPredicate(String key) {
        return MANIFEST_FIELDS.contains(key.toLowerCase());
    }

    private Optional<Manifest> getManifest(String manifestImplementationName) {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try(InputStream is = resources.nextElement().openStream()) {
                    Manifest manifest = new Manifest(is);
                    if (manifestImplementationName.equals(manifest.getMainAttributes().getValue(IMPLEMENTATION_TITLE))) {
                        return Optional.of(manifest);
                    }
                } catch (IOException e) {
                    LOG.warn(APIM_2016.pattern(), e);
                }
            }
        } catch (Exception ex) {
            LOG.warn(APIM_2017.pattern(), ex);
        }
        LOG.warn(APIM_2018.pattern(), IMPLEMENTATION_TITLE, manifestImplementationName);
        return Optional.empty();
    }

}
