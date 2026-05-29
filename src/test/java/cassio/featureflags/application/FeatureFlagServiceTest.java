package cassio.featureflags.application;

import cassio.featureflags.application.port.in.FeatureFlagUseCase.CreateFlagCommand;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.ListFlagsQuery;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.PatchFlagCommand;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.ServiceInfo;
import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.application.port.out.FlagEventPublisher;
import cassio.featureflags.domain.EvaluationContext;
import cassio.featureflags.domain.EvaluationResult;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.FlagType;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    private FeatureFlag booleanFlag(Long id, String name, String svc, boolean enabled) {
        return FeatureFlag.builder()
                .id(id)
                .flagName(name)
                .serviceName(svc)
                .type(FlagType.BOOLEAN)
                .environments(Map.of("prod", true))
                .tags(List.of())
                .enabled(enabled)
                .build();
    }

    @Test
    void shouldCreateFlagWithAllEnvironmentsDisabledRegardlessOfInput() {
        CreateFlagCommand cmd = new CreateFlagCommand(
                "checkout_v2", "billing", FlagType.ROLLOUT, 30,
                Map.of("production", true, "staging", true), List.of("payments"), "payments-team",
                LocalDate.of(2026, 9, 1));

        FeatureFlag saved = FeatureFlag.builder()
                .id(1L).flagName("checkout_v2").serviceName("billing")
                .type(FlagType.ROLLOUT).rollout(30)
                .environments(Map.of("production", false, "staging", false)).tags(List.of("payments"))
                .owner("payments-team").expiresAt(LocalDate.of(2026, 9, 1))
                .enabled(false).build();

        when(repository.existsByFlagName("checkout_v2")).thenReturn(false);
        when(repository.save(any())).thenReturn(saved);

        FeatureFlag result = service.create(cmd);

        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getEnvironments()).containsEntry("production", false).containsEntry("staging", false);

        ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
        verify(repository).save(captor.capture());
        // garante que o service forçou false em todos os ambientes, ignorando o que veio no comando
        assertThat(captor.getValue().getEnvironments())
                .containsEntry("production", false)
                .containsEntry("staging", false);
        verify(eventPublisher).publish(saved, FlagAction.CREATED);
    }

    @Test
    void shouldThrowWhenCreatingFlagWithDuplicateKey() {
        when(repository.existsByFlagName("dup")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateFlagCommand(
                "dup", "billing", FlagType.BOOLEAN, null, Map.of(), List.of(), null, null)))
                .isInstanceOf(FeatureFlagAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    // ── PATCH ─────────────────────────────────────────────────────────────────

    @Test
    void shouldPatchMetadataAndPublishUpdatedEvent() {
        FeatureFlag existing = booleanFlag(1L, "my-flag", "billing", false);
        FeatureFlag updated = existing.toBuilder().rollout(50).type(FlagType.ROLLOUT).build();
        PatchFlagCommand cmd = new PatchFlagCommand(FlagType.ROLLOUT, 50, null, null, null, null, null);

        when(repository.findByFlagName("my-flag")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(updated);

        service.patch("my-flag", cmd);

        verify(eventPublisher).publish(updated, FlagAction.UPDATED);
    }

    @Test
    void shouldPatchEnabledAndPublishToggledEvent() {
        FeatureFlag existing = booleanFlag(1L, "my-flag", "billing", false);
        FeatureFlag toggled = existing.toggleEnabled(true);
        PatchFlagCommand cmd = new PatchFlagCommand(null, null, null, null, null, null, true);

        when(repository.findByFlagName("my-flag")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(toggled);

        service.patch("my-flag", cmd);

        verify(eventPublisher).publish(toggled, FlagAction.TOGGLED);
    }

    @Test
    void shouldThrowWhenPatchingNonExistentFlag() {
        when(repository.findByFlagName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patch("ghost",
                new PatchFlagCommand(null, null, null, null, null, null, null)))
                .isInstanceOf(FeatureFlagNotFoundException.class);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void shouldDeleteByKeyAndPublishDeletedEvent() {
        FeatureFlag existing = booleanFlag(1L, "my-flag", "billing", false);
        when(repository.findByFlagName("my-flag")).thenReturn(Optional.of(existing));

        service.delete("my-flag");

        verify(repository).delete(existing);
        verify(eventPublisher).publish(existing, FlagAction.DELETED);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentFlag() {
        when(repository.findByFlagName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("ghost"))
                .isInstanceOf(FeatureFlagNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    @Test
    void shouldReturnFlagsMatchingServiceFilter() {
        List<FeatureFlag> flags = List.of(
                booleanFlag(1L, "flag-a", "billing", false),
                booleanFlag(2L, "flag-b", "billing", true));
        when(repository.findAll("billing", null, null, null)).thenReturn(flags);

        List<FeatureFlag> result = service.listFlags(new ListFlagsQuery("billing", null, null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoFlagsMatch() {
        when(repository.findAll(null, "dev", null, null)).thenReturn(List.of());

        assertThat(service.listFlags(new ListFlagsQuery(null, "dev", null, null))).isEmpty();
    }

    // ── EVALUATE ──────────────────────────────────────────────────────────────

    @Test
    void shouldEvaluateBooleanFlagEnabledDirectly() {
        FeatureFlag flag = booleanFlag(1L, "my-flag", "billing", true);
        when(repository.findByFlagName("my-flag")).thenReturn(Optional.of(flag));

        EvaluationResult result = service.evaluate("my-flag",
                EvaluationContext.of("user-1", "prod", Map.of()));

        assertThat(result.enabled()).isTrue();
        assertThat(result.type()).isEqualTo(FlagType.BOOLEAN);
    }

    @Test
    void shouldEvaluateFlagAsDisabledWhenEnvNotConfigured() {
        FeatureFlag flag = FeatureFlag.builder()
                .id(1L).flagName("my-flag").serviceName("billing")
                .type(FlagType.BOOLEAN).environments(Map.of("staging", true)).tags(List.of())
                .enabled(true).build();
        when(repository.findByFlagName("my-flag")).thenReturn(Optional.of(flag));

        EvaluationResult result = service.evaluate("my-flag",
                EvaluationContext.of("user-1", "prod", Map.of()));

        assertThat(result.enabled()).isFalse();
    }

    @Test
    void shouldThrowWhenEvaluatingNonExistentFlag() {
        when(repository.findByFlagName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate("ghost",
                EvaluationContext.of("u1", "prod", Map.of())))
                .isInstanceOf(FeatureFlagNotFoundException.class);
    }

    // ── EVALUATE BATCH ────────────────────────────────────────────────────────

    @Test
    void shouldEvaluateBatchAndReturnResultsKeyedByFlagName() {
        FeatureFlag flagA = booleanFlag(1L, "flag-a", "billing", true);
        FeatureFlag flagB = booleanFlag(2L, "flag-b", "billing", false);
        when(repository.findByFlagNameIn(List.of("flag-a", "flag-b")))
                .thenReturn(List.of(flagA, flagB));

        Map<String, EvaluationResult> results = service.evaluateBatch(
                List.of("flag-a", "flag-b"),
                EvaluationContext.of("user-1", "prod", Map.of()));

        assertThat(results).containsKeys("flag-a", "flag-b");
        assertThat(results.get("flag-a").enabled()).isTrue();
        assertThat(results.get("flag-b").enabled()).isFalse();
    }

    // ── GET SERVICES ──────────────────────────────────────────────────────────

    @Test
    void shouldReturnServicesGroupedWithTheirFlags() {
        when(repository.findDistinctServiceNames()).thenReturn(List.of("billing", "orders"));
        when(repository.findAll("billing", null, null, null))
                .thenReturn(List.of(booleanFlag(1L, "flag-a", "billing", true)));
        when(repository.findAll("orders", null, null, null))
                .thenReturn(List.of(booleanFlag(2L, "flag-b", "orders", false)));

        List<ServiceInfo> services = service.getServices();

        assertThat(services).hasSize(2);
        assertThat(services).extracting(ServiceInfo::serviceName)
                .containsExactlyInAnyOrder("billing", "orders");
        assertThat(services.stream()
                .filter(s -> s.serviceName().equals("billing"))
                .findFirst().orElseThrow().flags()).hasSize(1);
    }
}
