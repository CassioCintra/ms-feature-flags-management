package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.FlagType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagEventKafkaAdapterTest {

    private static final String TOPIC_FLAGS = "flag.events";

    @Mock
    private KafkaTemplate<String, FlagEvent> kafkaTemplate;

    @InjectMocks
    private FlagEventKafkaAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "topicFlags", TOPIC_FLAGS);
    }

    private FeatureFlag flag(String name, String svc) {
        return FeatureFlag.builder()
                .id(1L).flagName(name).serviceName(svc)
                .type(FlagType.BOOLEAN).environments(Map.of()).tags(List.of())
                .enabled(true).build();
    }

    @Test
    void shouldSendCreatedEventToFlagsTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.CREATED);

        verify(kafkaTemplate).send(TOPIC_FLAGS, "billing.my-flag", FlagEvent.from(f, FlagAction.CREATED));
    }

    @Test
    void shouldSendUpdatedEventToFlagsTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.UPDATED);

        verify(kafkaTemplate).send(TOPIC_FLAGS, "billing.my-flag", FlagEvent.from(f, FlagAction.UPDATED));
    }

    @Test
    void shouldSendToggledEventToFlagsTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.TOGGLED);

        verify(kafkaTemplate).send(TOPIC_FLAGS, "billing.my-flag", FlagEvent.from(f, FlagAction.TOGGLED));
    }

    @Test
    void shouldSendDeletedEventToFlagsTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.DELETED);

        verify(kafkaTemplate).send(TOPIC_FLAGS, "billing.my-flag", FlagEvent.from(f, FlagAction.DELETED));
    }
}
