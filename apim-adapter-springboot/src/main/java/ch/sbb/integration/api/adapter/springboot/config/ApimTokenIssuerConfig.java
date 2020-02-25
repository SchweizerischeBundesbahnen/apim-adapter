package ch.sbb.integration.api.adapter.springboot.config;

import ch.sbb.integration.api.adapter.config.TokenIssuerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Configuration
@ConfigurationProperties(prefix = "apim")
public class ApimTokenIssuerConfig {
    private List<TokenIssuerConfig> tokenissuer;

    public List<TokenIssuerConfig> getTokenissuer() {
        return tokenissuer;
    }

    public void setTokenissuer(List<TokenIssuerConfig> tokenissuer) {
        this.tokenissuer = tokenissuer;
    }
}
