package ch.sbb.integration.api.adapter.config;

import java.util.regex.Pattern;

public class TokenIssuerConfig {
    private String urlPattern;

    public TokenIssuerConfig() {
    }

    public TokenIssuerConfig(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public Pattern getUrlPatternCompiled() {
        return Pattern.compile(urlPattern);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TokenIssuerConfig{");
        sb.append("urlPattern='").append(urlPattern).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
