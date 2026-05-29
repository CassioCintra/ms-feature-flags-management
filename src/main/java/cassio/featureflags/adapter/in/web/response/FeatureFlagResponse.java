package cassio.featureflags.adapter.in.web.response;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;

public record FeatureFlagResponse(
        Long id,
        String flagName,
        String serviceName,
        FlagType type,
        Integer rollout,
        List<String> envs,
        List<String> tags,
        String owner,
        LocalDate expiresAt,
        boolean enabled
) {
    public static FeatureFlagResponse from(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(),
                flag.getFlagName(),
                flag.getServiceName(),
                flag.getType(),
                flag.getRollout(),
                flag.getEnvs(),
                flag.getTags(),
                flag.getOwner(),
                flag.getExpiresAt(),
                flag.isEnabled()
        );
    }
}
