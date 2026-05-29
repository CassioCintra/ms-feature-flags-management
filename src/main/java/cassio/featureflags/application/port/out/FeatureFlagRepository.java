package cassio.featureflags.application.port.out;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository {

    FeatureFlag save(FeatureFlag flag);

    Optional<FeatureFlag> findByFlagName(String flagName);

    boolean existsByFlagName(String flagName);

    void delete(FeatureFlag flag);

    List<FeatureFlag> findAll(String service, String env, FlagType type, String search);

    List<FeatureFlag> findByFlagNameIn(List<String> flagNames);

    List<String> findDistinctServiceNames();
}
