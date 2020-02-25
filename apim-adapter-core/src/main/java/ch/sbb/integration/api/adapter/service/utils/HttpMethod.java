package ch.sbb.integration.api.adapter.service.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2031;

/**
 * <p>Although not all methods may be used, we list all HTTP methods here for completeness.</p>
 * <a href="https://tools.ietf.org/html/rfc7231#section-4">rfc7231 section 4</a><br>
 * <pre>
 *    +---------+-------------------------------------------------+-------+
 *    | Method  | Description                                     | Sec.  |
 *    +---------+-------------------------------------------------+-------+
 *    | GET     | Transfer a current representation of the target | 4.3.1 |
 *    |         | resource.                                       |       |
 *    | HEAD    | Same as GET, but only transfer the status line  | 4.3.2 |
 *    |         | and header section.                             |       |
 *    | POST    | Perform resource-specific processing on the     | 4.3.3 |
 *    |         | request payload.                                |       |
 *    | PUT     | Replace all current representations of the      | 4.3.4 |
 *    |         | target resource with the request payload.       |       |
 *    | DELETE  | Remove all current representations of the       | 4.3.5 |
 *    |         | target resource.                                |       |
 *    | CONNECT | Establish a tunnel to the server identified by  | 4.3.6 |
 *    |         | the target resource.                            |       |
 *    | OPTIONS | Describe the communication options for the      | 4.3.7 |
 *    |         | target resource.                                |       |
 *    | TRACE   | Perform a message loop-back test along the path | 4.3.8 |
 *    |         | to the target resource.                         |       |
 *    +---------+-------------------------------------------------+-------+
 * </pre>
 * <a href="https://tools.ietf.org/html/rfc5789#section-2">rfc5789 section 2</a>
 * <pre>
 *    The PATCH method requests that a set of changes described in the
 *    request entity be applied to the resource identified by the Request-
 *    URI.  The set of changes is represented in a format called a "patch
 *    document" identified by a media type.  If the Request-URI does not
 *    point to an existing resource, the server MAY create a new resource,
 *    depending on the patch document type (whether it can logically modify
 *    a null resource) and permissions, etc.
 * </pre>
 */
public enum HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    CONNECT,
    OPTIONS,
    TRACE,
    PATCH;

    private static final Logger LOG = LoggerFactory.getLogger(HttpMethod.class);

    public static HttpMethod parse(String method) {
        if (method == null) {
            return null;
        }

        String normalizedMethod = method.trim().toUpperCase();
        for (HttpMethod httpMethod : values()) {
            if (httpMethod.name().equals(normalizedMethod)) {
                return httpMethod;
            }
        }

        LOG.warn(APIM_2031.pattern(), method);
        return null;
    }
}
