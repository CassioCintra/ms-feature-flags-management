package cassio.featureflags.adapter.in.web.config;

import cassio.featureflags.adapter.in.web.filter.TenantExtractorFilter;
import cassio.featureflags.adapter.in.web.filter.WorkspaceClaim;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TenantExtractorFilter tenantExtractorFilter;

    public SecurityConfig(TenantExtractorFilter tenantExtractorFilter) {
        this.tenantExtractorFilter = tenantExtractorFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs*", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().access((authentication, context) -> new AuthorizationDecision(
                                authentication.get() instanceof JwtAuthenticationToken jwt
                                        && WorkspaceClaim.of(jwt.getToken()).isPresent())))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .addFilterAfter(tenantExtractorFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }
}
