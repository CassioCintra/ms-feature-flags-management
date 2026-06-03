package cassio.featureflags.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelatorFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlator";
    public static final String MDC_KEY = "correlator";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String correlator = request.getHeader(HEADER);
        boolean propagated = correlator != null && !correlator.isBlank();
        if (!propagated) {
            correlator = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlator);
        response.setHeader(HEADER, correlator);

        String uri = buildUri(request);
        long start = System.currentTimeMillis();

        log.info("→ {} {}", request.getMethod(), uri);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException exception) {
            log.error("Request {} {} failed. Type: {}, message: {}",
                    request.getMethod(),
                    uri,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("← {} {} [Status:{} | {}ms]", request.getMethod(), uri, response.getStatus(), duration);
            MDC.remove(MDC_KEY);
        }
    }

    private String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? uri + "?" + query : uri;
    }
}

