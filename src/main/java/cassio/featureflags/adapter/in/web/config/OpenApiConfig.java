package cassio.featureflags.adapter.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${info.app.version}")
    private String version;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("ms-feature-flags").version(version))
                .servers(List.of(new Server()
                        .url("http://localhost:" + port + contextPath)
                        .description("Local")));
    }
}
