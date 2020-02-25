package ch.sbb.integration.api.adapter.filter;

import ch.sbb.integration.api.adapter.model.AuthRepResponse;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import ch.sbb.integration.api.adapter.service.utils.AuthUtils;
import ch.sbb.integration.api.adapter.service.utils.HttpMethod;
import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1002;

public class ApimAdapterFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ApimAdapterFilter.class);

    private ApimAdapterService apimAdapterService;

    private final List<HttpMethod> excludeFilterMethods;

    public ApimAdapterFilter(ApimAdapterService threeScaleAdapterService, List<HttpMethod> excludeFilterMethods) {
        this.apimAdapterService = threeScaleAdapterService;
        this.excludeFilterMethods = excludeFilterMethods;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        LOG.info(APIM_1002.pattern(), excludeFilterMethods);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        final HttpMethod httpMethod = HttpMethod.valueOf(httpRequest.getMethod().toUpperCase());

        final String token = AuthUtils.extractJwtFromAuthHeader(httpRequest.getHeader(AuthUtils.HTTP_AUTHORIZATION_HEADER_NAME));

        if (apimAdapterService.getApiWatch().isApiWatchRequest(token, httpMethod)) {
            apimAdapterService.getApiWatch().writeResponse(httpResponse);
        } else if (excludeFilterMethods.contains(httpMethod)) {
            filterChain.doFilter(servletRequest, httpResponse);
        } else {
            applyApimFilter(httpRequest, httpMethod, httpResponse, token, filterChain);
        }
    }

    private void applyApimFilter(HttpServletRequest httpRequest, HttpMethod httpMethod, HttpServletResponse httpResponse, String token, FilterChain filterChain) throws IOException, ServletException {
        StopWatch sw = new StopWatch().start();
        final String path = httpRequest.getRequestURI();
        final String queryString = httpRequest.getQueryString();

        final AuthRepResponse authRepResponse = apimAdapterService.authRep(token, path, queryString, httpMethod);

        if (authRepResponse.isAllowed()) {
            StopWatch responseSw = new StopWatch().start();
            filterChain.doFilter(httpRequest, httpResponse);
            LOG.debug("filter got response duration={} ms", responseSw.stop().getMillis());
        } else {
            if (authRepResponse.getWwwAuthenticateResponseHeader() != null) {
                httpResponse.setHeader(AuthUtils.HTTP_WWW_AUTHENTICATE_HEADER_NAME, authRepResponse.getWwwAuthenticateResponseHeader());
            }
            httpResponse.sendError(authRepResponse.getHttpStatus(), authRepResponse.getMessage());
        }

        apimAdapterService.reportHit(authRepResponse, httpResponse.getStatus());
        LOG.debug("APIM filter finished duration={} ms", sw.stop().getMillis());
    }

    @Override
    public void destroy() {
        apimAdapterService.close();
    }
}
