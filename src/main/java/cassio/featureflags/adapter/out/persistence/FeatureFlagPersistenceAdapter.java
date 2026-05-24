package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.domain.FeatureFlag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeatureFlagPersistenceAdapter implements FeatureFlagRepository {

    private final FeatureFlagJpaRepository jpaRepository;

    @Override
    public FeatureFlag save(FeatureFlag flag) {
        return jpaRepository.save(FeatureFlagEntity.from(flag)).toDomain();
    }

    @Override
    public Optional<FeatureFlag> findById(Long id) {
        return jpaRepository.findById(id).map(FeatureFlagEntity::toDomain);
    }

    @Override
    public boolean existsByFlagNameAndServiceNameAndEnvironmentName(String flagName, String serviceName, String environmentName) {
        return jpaRepository.existsByFlagNameAndServiceNameAndEnvironmentName(flagName, serviceName, environmentName);
    }

    @Override
    public void delete(FeatureFlag flag) {
        jpaRepository.deleteById(flag.getId());
    }

    @Override
    public List<FeatureFlag> findByServiceNameAndEnvironmentName(String serviceName, String environmentName) {
        return jpaRepository.findByServiceNameAndEnvironmentName(serviceName, environmentName)
                .stream()
                .map(FeatureFlagEntity::toDomain)
                .toList();
    }
}
