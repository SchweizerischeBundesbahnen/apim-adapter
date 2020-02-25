package ch.sbb.integration.api.adapter.model.tokenissuer;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.config.TokenIssuerConfig;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.sbb.integration.api.adapter.model.oidc.OidcConfigUtils.issuerToOpenIdConfigUrl;

public class TokenIssuerStore {
    private static final Logger LOG = LoggerFactory.getLogger(TokenIssuerStore.class);
    private static final Pattern MATCH_REGEX_GROUP_PATTERN = Pattern.compile(".*\\((.*)\\).*");

    private static TokenIssuerStore instance;

    public static TokenIssuerStore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TokenIssuerStore needs to be initialized first");
        }
        return instance;
    }

    public static TokenIssuerStore init(ApimAdapterConfig config, RestConfig restConfig, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
        instance = new TokenIssuerStore(config, restConfig, offlineConfigurationCacheRepo);
        instance.eagerIssuerInitialization();
        return instance;
    }

    public static void reset() {
        final TokenIssuerStore existingInstance = TokenIssuerStore.instance;
        TokenIssuerStore.instance = null;
        if (existingInstance != null) {
            existingInstance.close();
        }
    }

    static List<String> guessTokenIssuerUrls(String literal) {
        if (literal == null) {
            return Collections.singletonList(null);
        }
        final Set<String> issuers = new LinkedHashSet<>();

        final Matcher matcher = MATCH_REGEX_GROUP_PATTERN.matcher(literal);
        if (matcher.matches()) {
            if (matcher.groupCount() == 1) {
                for (String groupValue : matcher.group(1).split("\\|")) {
                    issuers.add(literal.substring(0, matcher.start(1) - 1) + groupValue + literal.substring(matcher.end(1) + 1));
                }
            } else if (matcher.groupCount() > 1) {
                throw new IllegalArgumentException("only one capturing group supported");
            }
        }
        if (issuers.isEmpty()) {
            issuers.add(literal);
        }
        // filter out patterns that still contains regex symbols -> this is needed here because we only want those issuer urls that can be seen from a pattern,
        // so for instance if we got "https://sso-dev.sbb.ch/auth/realms/(SBB_Public|SBB_Public2)" we know there are two valid issuer urls that will most certainly
        // exists and can be queries, whereas in case of "https://sso-dev.sbb.ch/auth/realms/(.*)" we would have no clue
        return issuers.stream().filter(s -> !containsRegex(s)).collect(Collectors.toList());
    }

    static boolean containsRegex(String s) {
        // very primitive approach,but for now it is probably okay
        return s != null && (s.contains("*") || s.contains("+") || s.contains("\\") || s.contains("[") || s.contains("]") || s.contains("{") || s.contains("}"));
    }

    private void eagerIssuerInitialization() {
        final Set<String> issuerUrls = issuerPatterns.stream()
                .map(Pattern::pattern)
                .map(TokenIssuerStore::guessTokenIssuerUrls)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        issuerUrls.stream().filter(Objects::nonNull).forEach(issuerUrl -> {
            try {
                resolve(issuerUrl);
            } catch (Exception e) {
                LOG.error(ReasonCode.APIM_3007.pattern(), issuerUrl, e);
            }
        });
    }

    private final ResteasyClient resteasyClient;
    private final OfflineConfigurationCacheRepo offlineConfigurationCacheRepo;
    private final Map<String, TokenIssuer> issuers = Collections.synchronizedMap(new HashMap<>());

    /* pattern is used for flexibility and in order to capture the realm / tenant name. If it does not contain
     * common char capturing, we can simply remove parentheses or the like and trigger an initial eager initialization
     * https://sso-dev.sbb.ch/auth/realms/(SBB_Public) -> https://sso-dev.sbb.ch/auth/realms/SBB_Public
     */
    private final List<Pattern> issuerPatterns;

    private TokenIssuerStore(ApimAdapterConfig config, RestConfig restConfig, OfflineConfigurationCacheRepo offlineConfigurationCacheRepo) {
        this.issuerPatterns = config.getTokenIssuers().stream().map(TokenIssuerConfig::getUrlPatternCompiled).collect(Collectors.toList());
        this.offlineConfigurationCacheRepo = offlineConfigurationCacheRepo;

        this.resteasyClient = restConfig.newRestEasyClient();
    }

    public Optional<TokenIssuer> resolve(String issuerUrl) {
        if (issuers.containsKey(issuerUrl)) {
            LOG.trace("Issuer resolved, return from store. IssuerURL={}", issuerUrl);
            return Optional.ofNullable(issuers.get(issuerUrl));
        }

        for (Pattern issuerPattern : issuerPatterns) {
            final Matcher matcher = issuerPattern.matcher(issuerUrl);
            if (matcher.matches()) {
                final TokenIssuer tokenIssuer = new TokenIssuer(resteasyClient, offlineConfigurationCacheRepo);
                // take host from token issuer URL, e.g. https://login.windows.net/2cda5d11-f0ac-46b3-967d-af1b2e1bd01a/ -> https://login.windows.net
                // therefore look for the first / after the scheme prefix 'https://'
                tokenIssuer.setIssuerUrl(issuerUrl);
                tokenIssuer.setHost(issuerUrl.substring(0, issuerUrl.indexOf('/', Math.min("https://".length(), issuerUrl.length()))));
                tokenIssuer.setRealm(matcher.groupCount() > 0 ? matcher.group(1) : "");
                tokenIssuer.setOidcUrl(issuerToOpenIdConfigUrl(issuerUrl));
                tokenIssuer.initJwks();
                issuers.put(issuerUrl, tokenIssuer);
                LOG.info(ReasonCode.APIM_1007.pattern(), issuerUrl);
                return Optional.of(tokenIssuer);
            }
        }

        LOG.warn(ReasonCode.APIM_2030.pattern(), issuerUrl);
        return Optional.empty();
    }

    public Collection<TokenIssuer> getIssuers() {
        return issuers.values();
    }

    private void close() {
        try {
            resteasyClient.close();
        } catch (Exception e) {
            LOG.error("Error during reset of TokenIssuerStore");
        }
    }
}
