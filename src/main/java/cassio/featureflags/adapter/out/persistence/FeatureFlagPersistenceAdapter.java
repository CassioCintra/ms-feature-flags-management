package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.adapter.out.persistence.entity.FeatureFlagEntity;
import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeatureFlagPersistenceAdapter implements FeatureFlagRepository {

    private final FeatureFlagJpaRepository jpaRepository;

    @Override
    public FeatureFlag save(FeatureFlag flag) {
        return jpaRepository.save(FeatureFlagEntity.from(flag)).toDomain();
    }

    @Override
    public Optional<FeatureFlag> findByFlagName(UUID workspaceId, String flagName) {
        return jpaRepository.findByWorkspaceIdAndFlagName(workspaceId, flagName).map(FeatureFlagEntity::toDomain);
    }

    @Override
    public boolean existsByFlagName(UUID workspaceId, String flagName) {
        return jpaRepository.existsByWorkspaceIdAndFlagName(workspaceId, flagName);
    }

    @Override
    public void delete(FeatureFlag flag) {
        jpaRepository.deleteById(flag.getId());
    }

    @Override
    public List<FeatureFlag> findAll(UUID workspaceId, String service, String env, FlagType type, String search) {
        return jpaRepository.findAllWithFilters(workspaceId, service, env, type, search)
                .stream()
                .map(FeatureFlagEntity::toDomain)
                .toList();
    }

    @Override
    public List<FeatureFlag> findAllAcrossWorkspaces() {
        return jpaRepository.findAll()
                .stream()
                .map(FeatureFlagEntity::toDomain)
                .toList();
    }

    @Override
    public List<FeatureFlag> findByFlagNameIn(UUID workspaceId, List<String> flagNames) {
        return jpaRepository.findByWorkspaceIdAndFlagNameIn(workspaceId, flagNames)
                .stream()
                .map(FeatureFlagEntity::toDomain)
                .toList();
    }

    @Override
    public List<String> findDistinctServiceNames(UUID workspaceId) {
        return jpaRepository.findDistinctServiceNames(workspaceId);
    }
}
