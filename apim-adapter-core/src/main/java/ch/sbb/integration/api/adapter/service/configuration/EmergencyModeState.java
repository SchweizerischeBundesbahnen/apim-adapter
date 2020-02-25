package ch.sbb.integration.api.adapter.service.configuration;

import ch.sbb.integration.api.adapter.model.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1015;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1016;

public class EmergencyModeState {
    private static final Logger LOG = LoggerFactory.getLogger(EmergencyModeState.class);

    private Map<ConfigType, Boolean> configurationSuccessfulLoaded = new ConcurrentHashMap<>();

    public EmergencyModeState() {
        for (ConfigType configType : ConfigType.values()) {
            configurationSuccessfulLoaded.put(configType, true);
        }
    }

    public void setConfigurationSuccessfulLoaded(ConfigType configType, boolean newState) {
        Boolean oldState = configurationSuccessfulLoaded.get(configType);

        if(oldState != newState) {
            LOG.info(APIM_1015.pattern(), configType.getId(), newState);

            boolean oldEmergencyMode = isEmergencyMode();
            configurationSuccessfulLoaded.put(configType, newState);
            boolean newEmergencyMode = isEmergencyMode();

            if(oldEmergencyMode !=  newEmergencyMode) {
                LOG.info(APIM_1016.pattern(), newEmergencyMode);
            }
        }
    }

    public boolean isConfigurationSuccessfulLoaded(ConfigType configType) {
        return configurationSuccessfulLoaded.getOrDefault(configType, false);
    }

    public boolean isEmergencyMode() {
        return !configurationSuccessfulLoaded
                .values()
                .stream()
                .reduce(true, (a, b) -> a && b);
    }
}
