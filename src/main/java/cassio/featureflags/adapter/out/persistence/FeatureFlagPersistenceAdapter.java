package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.adapter.out.persistence.entity.FeatureFlagEntity;
import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
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
    public Optional<FeatureFlag> findByFlagName(String flagName) {
        return jpaRepository.findByFlagName(flagName).map(FeatureFlagEntity::toDomain);
    }

    @Override
    public boolean existsByFlagName(String flagName) {
        return jpaRepository.existsByFlagName(flagName);
    }

    @Override
    public void delete(FeatureFlag flag) {
        jpaRepository.deleteById(flag.getId());
    }

    @Override
    public List<FeatureFlag> findAll(String service, String env, FlagType type, String search) {
        return jpaRepository.findAllWithFilters(service, env, type, search)
                .stream()
                .map(FeatureFlagEntity::toDomain)
                .toList();
    }

    @Override
    public List<FeatureFlag> findByFlagNameIn(List<String> flagNames) {
        return jpaRepository.findByFlagNameIn(flagNames)
                .stream()
                .map(FeatureFlagEntity::toDomain)
                .toList();
    }

    @Override
    public List<String> findDistinctServiceNames() {
        return jpaRepository.findDistinctServiceNames();
    }
}
