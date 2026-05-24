package cassio.featureflags.application.port.out;

import cassio.featureflags.domain.FeatureFlag;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository {

    FeatureFlag save(FeatureFlag flag);

    Optional<FeatureFlag> findById(Long id);

    boolean existsByFlagNameAndServiceNameAndEnvironmentName(String flagName, String serviceName, String environmentName);

    void delete(FeatureFlag flag);

    List<FeatureFlag> findByServiceNameAndEnvironmentName(String serviceName, String environmentName);
}
