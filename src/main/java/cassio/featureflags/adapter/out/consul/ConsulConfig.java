package cassio.featureflags.adapter.out.consul;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class ConsulConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                                                 OAuth2AuthorizedClientService authorizedClientService) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    public ConsulClient consulClient(@Value("${feature-flag.consul.url}") String url,
                                     @Value("${feature-flag.consul.auth-method}") String authMethod,
                                     @Value("${feature-flag.consul.client-registration-id}") String registrationId,
                                     OAuth2AuthorizedClientManager authorizedClientManager) {
        var request = OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                .principal("ms-feature-flags-management")
                .build();

        return new ConsulClient(RestClient.builder().baseUrl(url).build(), authMethod, () -> {
            var client = authorizedClientManager.authorize(request);
            if (client == null) {
                throw new IllegalStateException("Could not obtain a token for client registration '" + registrationId + "'");
            }
            return client.getAccessToken().getTokenValue();
        });
    }
}
