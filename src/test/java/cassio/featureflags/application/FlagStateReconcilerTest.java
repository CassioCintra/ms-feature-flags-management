package cassio.featureflags.application;

import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.application.port.out.FlagStateStore;
import cassio.featureflags.domain.FeatureFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagStateReconcilerTest {

    @Mock
    private FeatureFlagRepository repository;

    @Mock
    private FlagStateStore stateStore;

    @InjectMocks
    private FlagStateReconciler reconciler;

    private final FeatureFlag flag = FeatureFlag.builder().flagName("a").serviceName("billing").build();

    @Test
    void shouldSaveTheFlagWhenItChanges() {
        reconciler.onFlagStateChanged(new FlagStateChanged(flag, false));

        verify(stateStore).save(flag);
    }

    @Test
    void shouldRemoveTheFlagWhenItIsDeleted() {
        reconciler.onFlagStateChanged(new FlagStateChanged(flag, true));

        verify(stateStore).remove(flag);
    }

    @Test
    void shouldNotPropagateStoreFailuresAfterCommit() {
        doThrow(new IllegalStateException("Consul unavailable")).when(stateStore).save(flag);

        assertThatCode(() -> reconciler.onFlagStateChanged(new FlagStateChanged(flag, false))).doesNotThrowAnyException();
    }

    @Test
    void shouldReplaceTheStoredStateWithEveryFlagInTheDatabase() {
        var flags = List.of(flag, FeatureFlag.builder().flagName("b").serviceName("orders").build());
        when(repository.findAll(null, null, null, null)).thenReturn(flags);

        reconciler.reconcile();

        verify(stateStore).replaceAll(flags);
    }

    @Test
    void shouldKeepRunningWhenReconciliationFails() {
        when(repository.findAll(null, null, null, null)).thenReturn(List.of(flag));
        doThrow(new IllegalStateException("Consul unavailable")).when(stateStore).replaceAll(List.of(flag));

        assertThatCode(reconciler::reconcile).doesNotThrowAnyException();
    }
}
