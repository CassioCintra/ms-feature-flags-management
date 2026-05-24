package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.TestcontainersConfiguration;
import cassio.featureflags.domain.FeatureFlag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FeatureFlagPersistenceAdapter.class})
class FeatureFlagPersistenceAdapterTest {

    @Autowired
    private FeatureFlagPersistenceAdapter adapter;

    private FeatureFlag newFlag(String name, String svc, String env, boolean enabled) {
        return FeatureFlag.builder()
                .flagName(name).serviceName(svc).environmentName(env).enabled(enabled)
                .build();
    }

    @Test
    void shouldSaveAndReturnFlagWithGeneratedId() {
        FeatureFlag saved = adapter.save(newFlag("my-flag", "billing", "prod", false));

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getFlagName()).isEqualTo("my-flag");
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void shouldFindFlagById() {
        FeatureFlag saved = adapter.save(newFlag("find-flag", "svc", "dev", false));

        Optional<FeatureFlag> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFlagName()).isEqualTo("find-flag");
    }

    @Test
    void shouldReturnEmptyWhenFlagNotFound() {
        assertThat(adapter.findById(999L)).isEmpty();
    }

    @Test
    void shouldFindFlagsByServiceAndEnvironment() {
        adapter.save(newFlag("flag-a", "billing", "prod", true));
        adapter.save(newFlag("flag-b", "billing", "prod", false));
        adapter.save(newFlag("flag-c", "orders", "prod", true));

        List<FeatureFlag> result = adapter.findByServiceNameAndEnvironmentName("billing", "prod");

        assertThat(result).hasSize(2)
                .extracting(FeatureFlag::getServiceName)
                .containsOnly("billing");
    }

    @Test
    void shouldReturnTrueWhenFlagExists() {
        adapter.save(newFlag("existing", "svc", "env", true));

        assertThat(adapter.existsByFlagNameAndServiceNameAndEnvironmentName("existing", "svc", "env")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFlagDoesNotExist() {
        assertThat(adapter.existsByFlagNameAndServiceNameAndEnvironmentName("ghost", "svc", "env")).isFalse();
    }

    @Test
    void shouldDeleteFlag() {
        FeatureFlag saved = adapter.save(newFlag("delete-me", "svc", "dev", true));

        adapter.delete(saved);

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }
}
