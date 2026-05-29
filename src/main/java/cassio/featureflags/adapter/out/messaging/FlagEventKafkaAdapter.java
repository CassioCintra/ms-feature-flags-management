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

    @Value("${feature-flag.kafka.topics.flags}")
    private String topicFlags;

    private final KafkaTemplate<String, FlagEvent> kafkaTemplate;

    @Override
    public void publish(FeatureFlag flag, FlagAction action) {
        FlagEvent event = FlagEvent.from(flag, action);
        String key = flag.getServiceName() + "." + flag.getFlagName();
        log.info("Publishing event [topic={}, key={}, action={}]", topicFlags, key, action);
        kafkaTemplate.send(topicFlags, key, event);
    }
}
