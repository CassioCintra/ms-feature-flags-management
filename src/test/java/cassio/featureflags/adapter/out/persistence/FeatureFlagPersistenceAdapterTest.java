package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.TestcontainersConfiguration;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FeatureFlagPersistenceAdapter.class})
class FeatureFlagPersistenceAdapterTest {

    @Autowired
    private FeatureFlagPersistenceAdapter adapter;

    private FeatureFlag newFlag(String name, String svc, FlagType type, boolean enabled) {
        return FeatureFlag.builder()
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
                .flagName("checkout_v2").serviceName("billing")
                .type(FlagType.ROLLOUT).rollout(30)
                .environments(Map.of("production", true, "staging", false))
                .tags(List.of("payments", "checkout"))
                .owner("payments-team")
                .expiresAt(LocalDate.of(2026, 9, 1))
                .enabled(true).build();

        FeatureFlag saved = adapter.save(flag);

        assertThat(saved.getId()).isNotNull();
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

        Optional<FeatureFlag> found = adapter.findByFlagName("find-flag");

        assertThat(found).isPresent();
        assertThat(found.get().getFlagName()).isEqualTo("find-flag");
    }

    @Test
    void shouldReturnEmptyWhenFlagNotFound() {
        assertThat(adapter.findByFlagName("ghost")).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenFlagExists() {
        adapter.save(newFlag("existing", "svc", FlagType.BOOLEAN, true));

        assertThat(adapter.existsByFlagName("existing")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFlagDoesNotExist() {
        assertThat(adapter.existsByFlagName("ghost")).isFalse();
    }

    @Test
    void shouldDeleteFlag() {
        FeatureFlag saved = adapter.save(newFlag("delete-me", "svc", FlagType.BOOLEAN, true));

        adapter.delete(saved);

        assertThat(adapter.findByFlagName("delete-me")).isEmpty();
    }

    @Test
    void shouldFilterFlagsByService() {
        adapter.save(newFlag("flag-a", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("flag-b", "billing", FlagType.BOOLEAN, false));
        adapter.save(newFlag("flag-c", "orders",  FlagType.BOOLEAN, true));

        List<FeatureFlag> result = adapter.findAll("billing", null, null, null);

        assertThat(result).hasSize(2)
                .extracting(FeatureFlag::getServiceName)
                .containsOnly("billing");
    }

    @Test
    void shouldFilterFlagsByType() {
        adapter.save(newFlag("bool-flag",   "svc", FlagType.BOOLEAN,  true));
        adapter.save(newFlag("rollout-flag","svc", FlagType.ROLLOUT,  false));

        List<FeatureFlag> result = adapter.findAll(null, null, FlagType.ROLLOUT, null);

        assertThat(result).hasSize(1)
                .first().extracting(FeatureFlag::getFlagName).isEqualTo("rollout-flag");
    }

    @Test
    void shouldFilterFlagsBySearch() {
        adapter.save(newFlag("checkout_v2",  "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("new_pricing",  "billing", FlagType.BOOLEAN, false));

        List<FeatureFlag> result = adapter.findAll(null, null, null, "checkout");

        assertThat(result).hasSize(1)
                .first().extracting(FeatureFlag::getFlagName).isEqualTo("checkout_v2");
    }

    @Test
    void shouldFindFlagsByFlagNameIn() {
        adapter.save(newFlag("flag-a", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("flag-b", "billing", FlagType.BOOLEAN, false));
        adapter.save(newFlag("flag-c", "billing", FlagType.BOOLEAN, true));

        List<FeatureFlag> result = adapter.findByFlagNameIn(List.of("flag-a", "flag-c"));

        assertThat(result).hasSize(2)
                .extracting(FeatureFlag::getFlagName)
                .containsExactlyInAnyOrder("flag-a", "flag-c");
    }

    @Test
    void shouldReturnDistinctServiceNames() {
        adapter.save(newFlag("f1", "billing", FlagType.BOOLEAN, true));
        adapter.save(newFlag("f2", "billing", FlagType.BOOLEAN, false));
        adapter.save(newFlag("f3", "orders",  FlagType.BOOLEAN, true));

        List<String> services = adapter.findDistinctServiceNames();

        assertThat(services).containsExactlyInAnyOrder("billing", "orders");
    }
}
