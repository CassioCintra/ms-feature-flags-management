package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.TestcontainersConfiguration;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.FlagType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FlagEventOutboxAdapter.class, FlagEventOutboxAdapterTest.Json.class})
class FlagEventOutboxAdapterTest {

    private static final UUID WORKSPACE = UUID.fromString("7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70");

    @Autowired
    private FlagEventOutboxAdapter adapter;

    @Autowired
    private FlagEventOutboxRepository outbox;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void shouldStoreTheEventInTheOutboxInsteadOfSendingIt() {
        var flag = FeatureFlag.builder()
                .id(1L).workspaceId(WORKSPACE).flagName("checkout_v2").serviceName("billing")
                .type(FlagType.ROLLOUT).rollout(30)
                .environments(Map.of("prod", true)).tags(List.of("payments"))
                .owner("payments-team").expiresAt(LocalDate.of(2026, 9, 1))
                .enabled(true).build();

        adapter.publish(flag, FlagAction.TOGGLED, "user-123");

        var stored = outbox.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().getMessageKey()).isEqualTo(WORKSPACE + ".billing.checkout_v2");
        var event = jsonMapper.readValue(stored.getFirst().getPayload(), FlagEvent.class);
        assertThat(event).isEqualTo(FlagEvent.from(flag, FlagAction.TOGGLED, "user-123"));
        assertThat(event.workspaceId()).isEqualTo(WORKSPACE);
    }

    static class Json {

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }
}
