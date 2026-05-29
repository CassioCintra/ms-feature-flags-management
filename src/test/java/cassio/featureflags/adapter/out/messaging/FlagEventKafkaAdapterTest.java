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

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagEventKafkaAdapterTest {

    private static final String TOPIC_CREATED = "flag.created";
    private static final String TOPIC_UPDATED = "flag.updated";
    private static final String TOPIC_TOGGLED = "flag.toggled";
    private static final String TOPIC_DELETED = "flag.deleted";

    @Mock
    private KafkaTemplate<String, FlagEvent> kafkaTemplate;

    @InjectMocks
    private FlagEventKafkaAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "topicCreated", TOPIC_CREATED);
        ReflectionTestUtils.setField(adapter, "topicUpdated", TOPIC_UPDATED);
        ReflectionTestUtils.setField(adapter, "topicToggled", TOPIC_TOGGLED);
        ReflectionTestUtils.setField(adapter, "topicDeleted", TOPIC_DELETED);
    }

    private FeatureFlag flag(String name, String svc) {
        return FeatureFlag.builder()
                .id(1L).flagName(name).serviceName(svc)
                .type(FlagType.BOOLEAN).envs(List.of()).tags(List.of())
                .enabled(true).build();
    }

    @Test
    void shouldSendCreatedEventToCreatedTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.CREATED);

        verify(kafkaTemplate).send(TOPIC_CREATED, "billing.my-flag", FlagEvent.from(f, FlagAction.CREATED));
    }

    @Test
    void shouldSendUpdatedEventToUpdatedTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.UPDATED);

        verify(kafkaTemplate).send(TOPIC_UPDATED, "billing.my-flag", FlagEvent.from(f, FlagAction.UPDATED));
    }

    @Test
    void shouldSendToggledEventToToggledTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.TOGGLED);

        verify(kafkaTemplate).send(TOPIC_TOGGLED, "billing.my-flag", FlagEvent.from(f, FlagAction.TOGGLED));
    }

    @Test
    void shouldSendDeletedEventToDeletedTopic() {
        FeatureFlag f = flag("my-flag", "billing");

        adapter.publish(f, FlagAction.DELETED);

        verify(kafkaTemplate).send(TOPIC_DELETED, "billing.my-flag", FlagEvent.from(f, FlagAction.DELETED));
    }
}
