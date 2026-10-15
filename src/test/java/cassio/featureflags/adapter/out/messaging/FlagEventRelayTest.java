package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.FlagType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagEventRelayTest {

    private static final String TOPIC_FLAGS = "flag.events";

    @Mock
    private FlagEventOutboxRepository outbox;

    @Mock
    private KafkaTemplate<String, FlagEvent> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private FlagEventRelay relay;

    @BeforeEach
    void setUp() {
        relay = new FlagEventRelay(outbox, kafkaTemplate, jsonMapper);
        ReflectionTestUtils.setField(relay, "topicFlags", TOPIC_FLAGS);
    }

    @Test
    void shouldSendPendingEventsInOrderAndRemoveThem() {
        var created = pending(1L, "a", FlagAction.CREATED);
        var toggled = pending(2L, "a", FlagAction.TOGGLED);
        when(outbox.findTop100ByOrderByIdAsc()).thenReturn(List.of(created, toggled));
        when(kafkaTemplate.send(eq(TOPIC_FLAGS), eq("billing.a"), any())).thenReturn(sent());

        relay.relay();

        var order = inOrder(kafkaTemplate, outbox);
        order.verify(kafkaTemplate).send(TOPIC_FLAGS, "billing.a", event("a", FlagAction.CREATED));
        order.verify(outbox).delete(created);
        order.verify(kafkaTemplate).send(TOPIC_FLAGS, "billing.a", event("a", FlagAction.TOGGLED));
        order.verify(outbox).delete(toggled);
    }

    @Test
    void shouldStopAtTheFirstFailureAndKeepTheRemainingEvents() {
        var first = pending(1L, "a", FlagAction.CREATED);
        var failing = pending(2L, "b", FlagAction.CREATED);
        var last = pending(3L, "a", FlagAction.TOGGLED);
        when(outbox.findTop100ByOrderByIdAsc()).thenReturn(List.of(first, failing, last));
        when(kafkaTemplate.send(eq(TOPIC_FLAGS), eq("billing.a"), any())).thenReturn(sent());
        when(kafkaTemplate.send(eq(TOPIC_FLAGS), eq("billing.b"), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")));

        relay.relay();

        verify(outbox).delete(first);
        verify(outbox, never()).delete(failing);
        verify(outbox, never()).delete(last);
        verify(kafkaTemplate, never()).send(TOPIC_FLAGS, "billing.a", event("a", FlagAction.TOGGLED));
    }

    @Test
    void shouldDoNothingWhenTheOutboxIsEmpty() {
        when(outbox.findTop100ByOrderByIdAsc()).thenReturn(List.of());

        relay.relay();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    private FlagEventOutboxEntity pending(Long id, String flagName, FlagAction action) {
        var entity = new FlagEventOutboxEntity("billing." + flagName, jsonMapper.writeValueAsString(event(flagName, action)));
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static FlagEvent event(String flagName, FlagAction action) {
        var flag = FeatureFlag.builder()
                .id(1L).flagName(flagName).serviceName("billing")
                .type(FlagType.BOOLEAN).environments(Map.of()).tags(List.of())
                .enabled(true).build();
        return FlagEvent.from(flag, action, "user-123");
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<SendResult<String, FlagEvent>> sent() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }
}
