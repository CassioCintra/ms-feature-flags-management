package cassio.featureflags.adapter.in.web.response;

import cassio.featureflags.domain.FeatureFlag;

public record FeatureFlagResponse(
        Long id,
        String flagName,
        String serviceName,
        String environmentName,
        boolean enabled
) {
    public static FeatureFlagResponse from(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(),
                flag.getFlagName(),
                flag.getServiceName(),
                flag.getEnvironmentName(),
                flag.isEnabled()
        );
    }
}
