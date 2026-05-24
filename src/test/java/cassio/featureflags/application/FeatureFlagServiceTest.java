package cassio.featureflags.application;

import cassio.featureflags.application.port.in.FeatureFlagUseCase.CreateFlagCommand;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.UpdateFlagCommand;
import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.application.port.out.FlagEventPublisher;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository repository;

    @Mock
    private FlagEventPublisher eventPublisher;

    @InjectMocks
    private FeatureFlagService service;

    private FeatureFlag flag(Long id, String name, String svc, String env, boolean enabled) {
        return FeatureFlag.builder()
                .id(id).flagName(name).serviceName(svc).environmentName(env).enabled(enabled)
                .build();
    }

    @Test
    void shouldCreateFlagDisabledAndPublishEvent() {
        CreateFlagCommand cmd = new CreateFlagCommand("my-flag", "billing", "prod");
        FeatureFlag saved = flag(1L, "my-flag", "billing", "prod", false);

        when(repository.existsByFlagNameAndServiceNameAndEnvironmentName("my-flag", "billing", "prod")).thenReturn(false);
        when(repository.save(any())).thenReturn(saved);

        FeatureFlag result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isEnabled()).isFalse();

        ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();

        verify(eventPublisher).publish(saved, FlagAction.CREATED);
    }

    @Test
    void shouldThrowWhenFlagAlreadyExists() {
        when(repository.existsByFlagNameAndServiceNameAndEnvironmentName("dup", "svc", "env")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateFlagCommand("dup", "svc", "env")))
                .isInstanceOf(FeatureFlagAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateFlagAndPublishEvent() {
        FeatureFlag existing = flag(1L, "my-flag", "billing", "prod", false);
        FeatureFlag updated  = flag(1L, "my-flag", "billing", "prod", true);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(updated);

        FeatureFlag result = service.update(1L, new UpdateFlagCommand(true));

        assertThat(result.isEnabled()).isTrue();
        verify(eventPublisher).publish(updated, FlagAction.UPDATED);
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentFlag() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, new UpdateFlagCommand(true)))
                .isInstanceOf(FeatureFlagNotFoundException.class);
    }

    @Test
    void shouldDeleteFlagAndPublishEvent() {
        FeatureFlag existing = flag(1L, "my-flag", "billing", "prod", false);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
        verify(eventPublisher).publish(existing, FlagAction.DELETED);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentFlag() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(FeatureFlagNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void shouldReturnFlagsByServiceAndEnvironment() {
        List<FeatureFlag> flags = List.of(
                flag(1L, "flag-a", "billing", "prod", false),
                flag(2L, "flag-b", "billing", "prod", true)
        );
        when(repository.findByServiceNameAndEnvironmentName("billing", "prod")).thenReturn(flags);

        assertThat(service.findByServiceAndEnvironment("billing", "prod")).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoFlagsFound() {
        when(repository.findByServiceNameAndEnvironmentName("unknown", "dev")).thenReturn(List.of());

        assertThat(service.findByServiceAndEnvironment("unknown", "dev")).isEmpty();
    }
}
