package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.application.port.out.FlagEventPublisher;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagEventOutboxAdapter implements FlagEventPublisher {

    private final FlagEventOutboxRepository outbox;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(FeatureFlag flag, FlagAction action, String actorId) {
        var event = FlagEvent.from(flag, action, actorId);
        var key = flag.getServiceName() + "." + flag.getFlagName();
        outbox.save(new FlagEventOutboxEntity(key, jsonMapper.writeValueAsString(event)));
        log.debug("Event stored in outbox [key={}, action={}, actorId={}]", key, action, actorId);
    }
}
