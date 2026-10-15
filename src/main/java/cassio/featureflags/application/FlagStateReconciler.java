package cassio.featureflags.application;

import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.application.port.out.FlagStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagStateReconciler {

    private final FeatureFlagRepository repository;
    private final FlagStateStore stateStore;

    @TransactionalEventListener
    public void onFlagStateChanged(FlagStateChanged event) {
        var flag = event.flag();
        try {
            if (event.removed()) stateStore.remove(flag);
            else stateStore.save(flag);
        } catch (RuntimeException ex) {
            log.error("Flag state not synced, the next reconciliation will retry [flagName={}, error={}]",
                    flag.getFlagName(), ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${feature-flag.consul.reconcile-interval}")
    @Transactional(readOnly = true)
    public void reconcile() {
        try {
            var flags = repository.findAll(null, null, null, null);
            stateStore.replaceAll(flags);
            log.debug("Flag state reconciled [flags={}]", flags.size());
        } catch (RuntimeException ex) {
            log.error("Flag state reconciliation failed [error={}]", ex.getMessage());
        }
    }
}
