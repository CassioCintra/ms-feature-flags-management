package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.application.port.out.FlagEventPublisher;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagEventKafkaAdapter implements FlagEventPublisher {

    @Value("${feature-flag.kafka.topics.created}")
    private String topicCreated;

    @Value("${feature-flag.kafka.topics.updated}")
    private String topicUpdated;

    @Value("${feature-flag.kafka.topics.toggled}")
    private String topicToggled;

    @Value("${feature-flag.kafka.topics.deleted}")
    private String topicDeleted;

    private final KafkaTemplate<String, FlagEvent> kafkaTemplate;

    @Override
    public void publish(FeatureFlag flag, FlagAction action) {
        String topic = resolveTopic(action);
        FlagEvent event = FlagEvent.from(flag, action);
        String key = flag.getServiceName() + "." + flag.getFlagName();
        log.info("Publishing event [topic={}, key={}, action={}]", topic, key, action);
        kafkaTemplate.send(topic, key, event);
    }

    private String resolveTopic(FlagAction action) {
        return switch (action) {
            case CREATED -> topicCreated;
            case UPDATED -> topicUpdated;
            case TOGGLED -> topicToggled;
            case DELETED -> topicDeleted;
        };
    }
}
