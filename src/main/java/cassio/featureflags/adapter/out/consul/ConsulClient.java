package cassio.featureflags.adapter.out.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class ConsulClient {

    private static final int MAX_OPERATIONS_PER_TXN = 64;
    private static final ParameterizedTypeReference<List<KvEntry>> ENTRIES = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String authMethod;
    private final Supplier<String> jwtSupplier;
    private volatile String token;

    public Map<String, String> read(String prefix) {
        var entries = authorized(consulToken -> restClient.get()
                .uri(builder -> builder.path("/v1/kv/" + prefix).queryParam("recurse", true).build())
                .header("X-Consul-Token", consulToken)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {})
                .body(ENTRIES));

        var values = new HashMap<String, String>();
        if (entries == null) return values;
        for (var entry : entries) {
            var value = entry.value() == null ? "" : new String(Base64.getDecoder().decode(entry.value()), StandardCharsets.UTF_8);
            values.put(entry.key(), value);
        }
        return values;
    }

    public void write(Map<String, String> sets, Collection<String> deletes) {
        var operations = new ArrayList<Map<String, Object>>();
        sets.forEach((key, value) -> operations.add(Map.of("KV", Map.of(
                "Verb", "set",
                "Key", key,
                "Value", Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))))));
        deletes.forEach(key -> operations.add(Map.of("KV", Map.of("Verb", "delete", "Key", key))));

        for (var start = 0; start < operations.size(); start += MAX_OPERATIONS_PER_TXN) {
            var batch = operations.subList(start, Math.min(start + MAX_OPERATIONS_PER_TXN, operations.size()));
            authorized(consulToken -> restClient.put()
                    .uri("/v1/txn")
                    .header("X-Consul-Token", consulToken)
                    .body(batch)
                    .retrieve()
                    .toBodilessEntity());
        }
    }

    private <T> T authorized(Function<String, T> call) {
        if (token == null) token = login();
        try {
            return call.apply(token);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.info("Consul rejected the token, logging in again");
            token = login();
            return call.apply(token);
        }
    }

    private String login() {
        var response = restClient.post()
                .uri("/v1/acl/login")
                .body(Map.of("AuthMethod", authMethod, "BearerToken", jwtSupplier.get()))
                .retrieve()
                .body(LoginResponse.class);
        if (response == null || response.secretId() == null) {
            throw new IllegalStateException("Consul login returned no token");
        }
        log.debug("Logged in to Consul [authMethod={}]", authMethod);
        return response.secretId();
    }

    record KvEntry(@JsonProperty("Key") String key, @JsonProperty("Value") String value) {}

    record LoginResponse(@JsonProperty("SecretID") String secretId) {}
}
