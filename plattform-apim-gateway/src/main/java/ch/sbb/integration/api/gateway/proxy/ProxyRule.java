package ch.sbb.integration.api.gateway.proxy;

import java.util.regex.Pattern;

/**
 * Represents a proxy rule. the location matches the request path. rewrite contains the rewrite rule
 * (with placeholders) and proxyPass holds the backend server with protocol and port. rewrite and proxyPass
 * can contain replacement variables from matches of the location pattern.
 *
 * Example:
 * location: ^/foo/bar/(.*)/etc$
 * rewrite:  /new/foo/$1/bar
 * proxyPass http://backend:8080
 *
 * @author u223622
 */
public class ProxyRule {
    private String location = null;
    private Pattern locationPattern = null;
    private String rewrite = null;
    private String proxyPass = null;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        this.locationPattern = Pattern.compile(location);
    }

    public Pattern getLocationPattern() {
        return locationPattern;
    }

    public String getRewrite() {
        return rewrite;
    }

    public void setRewrite(String rewrite) {
        this.rewrite = rewrite;
    }

    public String getProxyPass() {
        return proxyPass;
    }

    public void setProxyPass(String proxyPass) {
        this.proxyPass = proxyPass;
    }
}
