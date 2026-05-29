package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;

public record FlagEvent(
        String flagName,
        String serviceName,
        FlagType type,
        Integer rollout,
        List<String> envs,
        List<String> tags,
        String owner,
        LocalDate expiresAt,
        boolean enabled,
        FlagAction action
) {
    public static FlagEvent from(FeatureFlag flag, FlagAction action) {
        return new FlagEvent(
                flag.getFlagName(),
                flag.getServiceName(),
                flag.getType(),
                flag.getRollout(),
                flag.getEnvs(),
                flag.getTags(),
                flag.getOwner(),
                flag.getExpiresAt(),
                flag.isEnabled(),
                action
        );
    }
}
