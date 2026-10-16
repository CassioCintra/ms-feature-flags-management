package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.TestcontainersConfiguration;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FeatureFlagPersistenceAdapter.class})
class FeatureFlagPersistenceAdapterTest {

    private static final UUID WORKSPACE = UUID.fromString("7a1c0e2e-4d5b-4c8e-9f10-2b3c4d5e6f70");
    private static final UUID OTHER_WORKSPACE = UUID.fromString("0b9d8c7a-6e5f-4a3b-8c2d-1e0f9a8b7c6d");

    @Autowired
    private FeatureFlagPersistenceAdapter adapter;

    private FeatureFlag newFlag(String name, String svc, FlagType type, boolean enabled) {
        return newFlag(WORKSPACE, name, svc, type, enabled);
    }

    private FeatureFlag newFlag(UUID workspaceId, String name, String svc, FlagType type, boolean enabled) {
        return FeatureFlag.builder()
                .workspaceId(workspaceId)
                .flagName(name).serviceName(svc)
                .type(type).environments(Map.of("prod", true)).tags(List.of("t1"))
                .enabled(enabled).build();
    }

    @Test
    void shouldSaveAndReturnFlagWithGeneratedId() {
        FeatureFlag saved = adapter.save(newFlag("my-flag", "billing", FlagType.BOOLEAN, false));

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getFlagName()).isEqualTo("my-flag");
        assertThat(saved.getType()).isEqualTo(FlagType.BOOLEAN);
        assertThat(saved.getEnvironments()).containsEntry("prod", true);
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void shouldSaveEnrichedFlagAndRoundTripAllFields() {
        FeatureFlag flag = FeatureFlag.builder()
                .workspaceId(WORKSPACE)
                .flagName("checkout_v2").serviceName("billing")
                .type(FlagType.ROLLOUT).rollout(30)
                .environments(Map.of("production", true, "staging", false))
                .tags(List.of("payments", "checkout"))
                .owner("payments-team")
                .expiresAt(LocalDate.of(2026, 9, 1))
                .enabled(true).build();

        FeatureFlag saved = adapter.save(flag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getWorkspaceId()).isEqualTo(WORKSPACE);
        assertThat(saved.getType()).isEqualTo(FlagType.ROLLOUT);
        assertThat(saved.getRollout()).isEqualTo(30);
        assertThat(saved.getEnvironments()).containsEntry("production", true).containsEntry("staging", false);
        assertThat(saved.getTags()).containsExactlyInAnyOrder("payments", "checkout");
        assertThat(saved.getOwner()).isEqualTo("payments-team");
        assertThat(saved.getExpiresAt()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void shouldFindFlagByFlagName() {
        adapter.save(newFlag("find-flag", "svc", FlagType.BOOLEAN, false));

        Optional<FeatureFlag> found = adapter.findByFlagName(WORKSPACE, "find-flag");

        assertThat(found).isPresent();
        assertThat(found.get().getFlagName()).isEqualTo("find-flag");
    }

    @Test
    void shouldReturnEmptyWhenFlagNotFound() {
        assertThat(adapter.findByFlagName(WORKSPACE, "ghost")).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenFlagExists() {
        adapter.save(newFlag("existing", "svc", FlagType.BOOLEAN, true));

        assertThat(adapter.existsByFlagName(WORKSPACE, "existing")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFlagDoesNotExist() {
        assertThat(adapter.existsByFlagName(WORKSPACE, "ghost")).isFalse();
    }

    @Test
    void shouldDeleteFlag() {
        FeatureFlag saved = adapter.save(newFlag("delete-me", "svc", FlagType.BOOLEAN, true));

        adapter.delete(saved);

        assertThat(adapter.findByFlagName(WORKSPACE, "delete-me")).isEmpty();
    }

    @Test
    void shouldFilterFlagsByService() {
        adapter.save(newFlag("flag-a", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("flag-b", "billing", FlagType.BOOLEAN, false));
        adapter.save(newFlag("flag-c", "orders",  FlagType.BOOLEAN, true));

        List<FeatureFlag> result = adapter.findAll(WORKSPACE, "billing", null, null, null);

        assertThat(result).hasSize(2)
                .extracting(FeatureFlag::getServiceName)
                .containsOnly("billing");
    }

    @Test
    void shouldFilterFlagsByType() {
        adapter.save(newFlag("bool-flag",   "svc", FlagType.BOOLEAN,  true));
        adapter.save(newFlag("rollout-flag","svc", FlagType.ROLLOUT,  false));

        List<FeatureFlag> result = adapter.findAll(WORKSPACE, null, null, FlagType.ROLLOUT, null);

        assertThat(result).hasSize(1)
                .first().extracting(FeatureFlag::getFlagName).isEqualTo("rollout-flag");
    }

    @Test
    void shouldFilterFlagsBySearch() {
        adapter.save(newFlag("checkout_v2",  "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("new_pricing",  "billing", FlagType.BOOLEAN, false));

        List<FeatureFlag> result = adapter.findAll(WORKSPACE, null, null, null, "checkout");

        assertThat(result).hasSize(1)
                .first().extracting(FeatureFlag::getFlagName).isEqualTo("checkout_v2");
    }

    @Test
    void shouldFindFlagsByFlagNameIn() {
        adapter.save(newFlag("flag-a", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("flag-b", "billing", FlagType.BOOLEAN, false));
        adapter.save(newFlag("flag-c", "billing", FlagType.BOOLEAN, true));

        List<FeatureFlag> result = adapter.findByFlagNameIn(WORKSPACE, List.of("flag-a", "flag-c"));

        assertThat(result).hasSize(2)
                .extracting(FeatureFlag::getFlagName)
                .containsExactlyInAnyOrder("flag-a", "flag-c");
    }

    @Test
    void shouldReturnDistinctServiceNames() {
        adapter.save(newFlag("f1", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("f2", "billing", FlagType.BOOLEAN, false));
        adapter.save(newFlag("f3", "orders",  FlagType.BOOLEAN, true));

        List<String> services = adapter.findDistinctServiceNames(WORKSPACE);

        assertThat(services).containsExactlyInAnyOrder("billing", "orders");
    }

    @Test
    void shouldAllowTheSameFlagNameInDifferentWorkspaces() {
        adapter.save(newFlag(WORKSPACE, "checkout", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag(OTHER_WORKSPACE, "checkout", "billing", FlagType.BOOLEAN, false));

        assertThat(adapter.findByFlagName(WORKSPACE, "checkout")).get().extracting(FeatureFlag::isEnabled).isEqualTo(true);
        assertThat(adapter.findByFlagName(OTHER_WORKSPACE, "checkout")).get().extracting(FeatureFlag::isEnabled).isEqualTo(false);
    }

    @Test
    void shouldRejectTheSameFlagNameTwiceInOneWorkspace() {
        adapter.save(newFlag("checkout", "billing", FlagType.BOOLEAN, true));

        assertThatThrownBy(() -> adapter.save(newFlag("checkout", "orders", FlagType.BOOLEAN, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldNotExposeFlagsOfAnotherWorkspace() {
        adapter.save(newFlag(OTHER_WORKSPACE, "secret", "billing", FlagType.BOOLEAN, true));

        assertThat(adapter.findByFlagName(WORKSPACE, "secret")).isEmpty();
        assertThat(adapter.existsByFlagName(WORKSPACE, "secret")).isFalse();
        assertThat(adapter.findAll(WORKSPACE, null, null, null, null)).isEmpty();
        assertThat(adapter.findByFlagNameIn(WORKSPACE, List.of("secret"))).isEmpty();
        assertThat(adapter.findDistinctServiceNames(WORKSPACE)).isEmpty();
    }

    @Test
    void shouldFindFlagsOfEveryWorkspaceForReconciliation() {
        adapter.save(newFlag(WORKSPACE, "flag-a", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag(OTHER_WORKSPACE, "flag-b", "orders", FlagType.BOOLEAN, true));

        assertThat(adapter.findAllAcrossWorkspaces())
                .extracting(FeatureFlag::getWorkspaceId)
                .containsExactlyInAnyOrder(WORKSPACE, OTHER_WORKSPACE);
    }
}
