package cassio.featureflags.application.port.in;

import cassio.featureflags.domain.FeatureFlag;

import java.util.List;

public interface FeatureFlagUseCase {

    FeatureFlag create(CreateFlagCommand command);

    FeatureFlag update(Long id, UpdateFlagCommand command);

    void delete(Long id);

    List<FeatureFlag> findByServiceAndEnvironment(String serviceName, String environmentName);

    record CreateFlagCommand(String flagName, String serviceName, String environmentName) {}

    record UpdateFlagCommand(boolean enabled) {}
}
