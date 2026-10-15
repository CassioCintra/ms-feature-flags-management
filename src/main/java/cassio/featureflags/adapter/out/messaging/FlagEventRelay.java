package cassio.featureflags.adapter.out.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagEventRelay {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    @Value("${feature-flag.kafka.topics.flags}")
    private String topicFlags;

    private final FlagEventOutboxRepository outbox;
    private final KafkaTemplate<String, FlagEvent> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Scheduled(fixedDelayString = "${feature-flag.kafka.relay-interval}")
    @Transactional
    public void relay() {
        for (var pending : outbox.findTop100ByOrderByIdAsc()) {
            try {
                var event = jsonMapper.readValue(pending.getPayload(), FlagEvent.class);
                kafkaTemplate.send(topicFlags, pending.getMessageKey(), event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ex) {
                log.warn("Event relay stopped, will retry [outboxId={}, error={}]", pending.getId(), ex.getMessage());
                return;
            }
            outbox.delete(pending);
            log.info("Event published [topic={}, key={}, outboxId={}]", topicFlags, pending.getMessageKey(), pending.getId());
        }
    }
}
