package cassio.featureflags.adapter.out.consul;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class ConsulFlagStateAdapterTest {

    private static final String MANAGEMENT_TOKEN = "root";
    private static final List<String> ENVIRONMENTS = List.of("dev", "hml", "prod");

    @Container
    static final ConsulContainer CONSUL = new ConsulContainer("hashicorp/consul:2.0.4")
            .withEnv("CONSUL_LOCAL_CONFIG", """
                    {"acl": {"enabled": true, "default_policy": "deny",
                             "tokens": {"initial_management": "%s"}}}""".formatted(MANAGEMENT_TOKEN));

    private static RSAKey signingKey;
    private static RestClient admin;

    private final UUID workspace = UUID.randomUUID();
    private final String service = "svc-" + UUID.randomUUID();
    private final AtomicInteger logins = new AtomicInteger();
    private ConsulFlagStateAdapter adapter;

    @BeforeAll
    static void configureAcl() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048).generate();
        var publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(signingKey.toRSAPublicKey().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        admin = RestClient.builder()
                .baseUrl(consulUrl())
                .defaultHeader("X-Consul-Token", MANAGEMENT_TOKEN)
                .build();
        admin.put().uri("/v1/acl/policy")
                .body(Map.of("Name", "flags-write", "Rules", "key_prefix \"feature-flags/\" { policy = \"write\" }"))
                .retrieve().toBodilessEntity();
        admin.put().uri("/v1/acl/role")
                .body(Map.of("Name", "flags-writer", "Policies", List.of(Map.of("Name", "flags-write"))))
                .retrieve().toBodilessEntity();
        admin.put().uri("/v1/acl/auth-method")
                .body(Map.of("Name", "ms-auth", "Type", "jwt", "MaxTokenTTL", "15m", "Config", Map.of(
                        "JWTValidationPubKeys", List.of(publicKeyPem),
                        "BoundAudiences", List.of("consul"),
                        "ListClaimMappings", Map.of("scope", "scopes"),
                        "ClaimMappings", Map.of("sub", "client_id"))))
                .retrieve().toBodilessEntity();
        admin.put().uri("/v1/acl/binding-rule")
                .body(Map.of("AuthMethod", "ms-auth", "BindType", "role", "BindName", "flags-writer",
                        "Selector", "\"flags:write\" in list.scopes"))
                .retrieve().toBodilessEntity();
    }

    @BeforeEach
    void createAdapter() {
        var client = new ConsulClient(RestClient.builder().baseUrl(consulUrl()).build(), "ms-auth", () -> {
            logins.incrementAndGet();
            return jwt();
        });
        adapter = new ConsulFlagStateAdapter(client, ENVIRONMENTS);
    }

    @Test
    void writesTheEffectiveStateOfTheFlagInEveryEnvironment() {
        adapter.save(flag("all-envs", true, Map.of()));
        adapter.save(flag("dev-and-qa", true, Map.of("dev", true, "qa", true, "prod", false)));
        adapter.save(flag("switched-off", false, Map.of("dev", true)));

        assertThat(serviceKeys()).containsOnly(
                Map.entry("dev/all-envs", "{\"enabled\":true}"),
                Map.entry("hml/all-envs", "{\"enabled\":true}"),
                Map.entry("prod/all-envs", "{\"enabled\":true}"),
                Map.entry("dev/dev-and-qa", "{\"enabled\":true}"),
                Map.entry("qa/dev-and-qa", "{\"enabled\":true}"),
                Map.entry("hml/dev-and-qa", "{\"enabled\":false}"),
                Map.entry("prod/dev-and-qa", "{\"enabled\":false}"),
                Map.entry("dev/switched-off", "{\"enabled\":false}"),
                Map.entry("hml/switched-off", "{\"enabled\":false}"),
                Map.entry("prod/switched-off", "{\"enabled\":false}"));
    }

    @Test
    void removesEnvironmentsThatTheFlagNoLongerHas() {
        adapter.save(flag("new-checkout", true, Map.of("qa", true)));
        adapter.save(flag("new-checkout", true, Map.of("dev", true)));

        assertThat(serviceKeys()).containsOnlyKeys("dev/new-checkout", "hml/new-checkout", "prod/new-checkout");
    }

    @Test
    void removesTheFlagFromEveryEnvironmentAndKeepsTheOthers() {
        adapter.save(flag("new-checkout", true, Map.of("qa", true)));
        adapter.save(flag("new-checkout-v2", true, Map.of()));

        adapter.remove(flag("new-checkout", true, Map.of()));

        assertThat(serviceKeys()).containsOnlyKeys("dev/new-checkout-v2", "hml/new-checkout-v2", "prod/new-checkout-v2");
    }

    @Test
    void replaceAllMakesConsulMatchTheDatabaseAndKeepsCorrectKeysUntouched() {
        adapter.save(flag("unchanged", true, Map.of()));
        var unchangedIndex = modifyIndex("dev/unchanged");
        putKey("dev/outdated", "{\"enabled\":false}");
        putKey("dev/orphan", "{\"enabled\":true}");
        putKey("dev/broken", "not json");

        adapter.replaceAll(List.of(flag("unchanged", true, Map.of()), flag("outdated", true, Map.of())));

        assertThat(serviceKeys()).containsOnly(
                Map.entry("dev/unchanged", "{\"enabled\":true}"),
                Map.entry("hml/unchanged", "{\"enabled\":true}"),
                Map.entry("prod/unchanged", "{\"enabled\":true}"),
                Map.entry("dev/outdated", "{\"enabled\":true}"),
                Map.entry("hml/outdated", "{\"enabled\":true}"),
                Map.entry("prod/outdated", "{\"enabled\":true}"));
        assertThat(modifyIndex("dev/unchanged")).isEqualTo(unchangedIndex);
    }

    @Test
    void replaceAllSplitsLargeChangesIntoSeveralTransactions() {
        var flags = IntStream.range(0, 40).mapToObj(i -> flag("flag-" + i, true, Map.of())).toList();

        adapter.replaceAll(flags);

        assertThat(serviceKeys()).hasSize(120);
    }

    @Test
    void logsInAgainWhenConsulRejectsTheToken() {
        adapter.save(flag("new-checkout", false, Map.of()));
        assertThat(logins).hasValue(1);

        var tokens = admin.get().uri("/v1/acl/tokens?authmethod=ms-auth")
                .retrieve().body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        tokens.forEach(token -> admin.delete().uri("/v1/acl/token/" + token.get("AccessorID"))
                .retrieve().toBodilessEntity());

        adapter.save(flag("new-checkout", true, Map.of()));

        assertThat(logins).hasValue(2);
        assertThat(serviceKeys()).containsEntry("dev/new-checkout", "{\"enabled\":true}");
    }

    @Test
    void keepsFlagsWithTheSameNameInDifferentWorkspacesApart() {
        var otherWorkspace = UUID.randomUUID();
        adapter.save(flag("new-checkout", true, Map.of()));
        adapter.save(flag(otherWorkspace, "new-checkout", false, Map.of()));

        adapter.remove(flag("new-checkout", true, Map.of()));

        assertThat(serviceKeys()).isEmpty();
        assertThat(serviceKeys(otherWorkspace)).containsOnly(
                Map.entry("dev/new-checkout", "{\"enabled\":false}"),
                Map.entry("hml/new-checkout", "{\"enabled\":false}"),
                Map.entry("prod/new-checkout", "{\"enabled\":false}"));
    }

    @Test
    void replaceAllRemovesKeysOfTheLayoutWithoutWorkspace() {
        admin.put().uri("/v1/kv/feature-flags/" + service + "/dev/legacy").body("{\"enabled\":true}").retrieve().toBodilessEntity();

        adapter.replaceAll(List.of(flag("current", true, Map.of())));

        var legacy = admin.get().uri("/v1/kv/feature-flags/" + service + "/?recurse")
                .exchange((request, response) -> response.getStatusCode().value());
        assertThat(legacy).isEqualTo(404);
        assertThat(serviceKeys()).containsOnlyKeys("dev/current", "hml/current", "prod/current");
    }

    private FeatureFlag flag(String name, boolean enabled, Map<String, Boolean> environments) {
        return flag(workspace, name, enabled, environments);
    }

    private FeatureFlag flag(UUID workspaceId, String name, boolean enabled, Map<String, Boolean> environments) {
        return FeatureFlag.builder()
                .workspaceId(workspaceId)
                .flagName(name)
                .serviceName(service)
                .type(FlagType.BOOLEAN)
                .environments(environments)
                .enabled(enabled)
                .build();
    }

    private Map<String, String> serviceKeys() {
        return serviceKeys(workspace);
    }

    private Map<String, String> serviceKeys(UUID workspaceId) {
        var prefix = "feature-flags/" + workspaceId + "/" + service + "/";
        var entries = admin.get().uri("/v1/kv/" + prefix + "?recurse")
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {})
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        if (entries == null) return Map.of();
        return entries.stream().collect(Collectors.toMap(
                entry -> ((String) entry.get("Key")).substring(prefix.length()),
                entry -> new String(Base64.getDecoder().decode((String) entry.get("Value")))));
    }

    private long modifyIndex(String key) {
        var entries = admin.get().uri("/v1/kv/feature-flags/" + workspace + "/" + service + "/" + key)
                .retrieve().body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return ((Number) entries.getFirst().get("ModifyIndex")).longValue();
    }

    private void putKey(String key, String value) {
        admin.put().uri("/v1/kv/feature-flags/" + workspace + "/" + service + "/" + key).body(value).retrieve().toBodilessEntity();
    }

    private static String jwt() {
        try {
            var claims = new JWTClaimsSet.Builder()
                    .subject("ms-feature-flags-management")
                    .audience("consul")
                    .claim("scope", List.of("flags:write"))
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String consulUrl() {
        return "http://" + CONSUL.getHost() + ":" + CONSUL.getMappedPort(8500);
    }
}
