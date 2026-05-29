package cassio.featureflags.adapter.in.web.response;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FeatureFlagResponse(
        Long id,
        String flagName,
        String serviceName,
        FlagType type,
        Integer rollout,
        Map<String, Boolean> environments,
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
                flag.getEnvironments(),
                flag.getTags(),
                flag.getOwner(),
                flag.getExpiresAt(),
                flag.isEnabled()
        );
    }
}
