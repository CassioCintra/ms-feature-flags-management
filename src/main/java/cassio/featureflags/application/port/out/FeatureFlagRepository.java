package cassio.featureflags.application.port.out;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository {

    FeatureFlag save(FeatureFlag flag);

    Optional<FeatureFlag> findByFlagName(UUID workspaceId, String flagName);

    boolean existsByFlagName(UUID workspaceId, String flagName);

    void delete(FeatureFlag flag);

    List<FeatureFlag> findAll(UUID workspaceId, String service, String env, FlagType type, String search);

    List<FeatureFlag> findAllAcrossWorkspaces();

    List<FeatureFlag> findByFlagNameIn(UUID workspaceId, List<String> flagNames);

    List<String> findDistinctServiceNames(UUID workspaceId);
}
