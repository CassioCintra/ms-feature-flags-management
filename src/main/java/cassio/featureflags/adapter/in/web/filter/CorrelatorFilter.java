package cassio.featureflags.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelatorFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Correlator";
    static final String MDC_KEY = "correlator";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlator = request.getHeader(HEADER);
        if (correlator == null || correlator.isBlank()) {
            correlator = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlator);
        response.setHeader(HEADER, correlator);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
