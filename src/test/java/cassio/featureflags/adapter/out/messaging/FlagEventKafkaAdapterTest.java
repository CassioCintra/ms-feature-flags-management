package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagEventKafkaAdapterTest {

    private static final String TOPIC = "feature-flags.events";

    @Mock
    private KafkaTemplate<String, FlagEvent> kafkaTemplate;

    @InjectMocks
    private FlagEventKafkaAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "topic", TOPIC);
    }

    @Test
    void shouldSendEventToCorrectTopicWithCompositeKey() {
        FeatureFlag flag = FeatureFlag.builder()
                .id(1L).flagName("my-flag")
                .serviceName("billing")
                .environmentName("production")
                .enabled(true)
                .build();

        adapter.publish(flag, FlagAction.CREATED);

        verify(kafkaTemplate).send(
                TOPIC,
                "billing.production",
                FlagEvent.from(flag, FlagAction.CREATED)
        );
    }

    @Test
    void shouldCompositeKeyFromServiceAndEnvironment() {
        FeatureFlag flag = FeatureFlag.builder()
                .id(2L).flagName("flag")
                .serviceName("orders")
                .environmentName("staging")
                .enabled(false)
                .build();

        adapter.publish(flag, FlagAction.UPDATED);

        verify(kafkaTemplate).send(
                TOPIC,
                "orders.staging",
                FlagEvent.from(flag, FlagAction.UPDATED)
        );
    }
}
