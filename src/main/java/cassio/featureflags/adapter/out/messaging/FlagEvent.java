package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FlagEvent(
        UUID workspaceId,
        String flagName,
        String serviceName,
        FlagType type,
        Integer rollout,
        Map<String, Boolean> environments,
        List<String> tags,
        String owner,
        LocalDate expiresAt,
        boolean enabled,
        FlagAction action,
        String actorId
) {
    public static FlagEvent from(FeatureFlag flag, FlagAction action, String actorId) {
        return new FlagEvent(
                flag.getWorkspaceId(),
                flag.getFlagName(),
                flag.getServiceName(),
                flag.getType(),
                flag.getRollout(),
                flag.getEnvironments(),
                flag.getTags(),
                flag.getOwner(),
                flag.getExpiresAt(),
                flag.isEnabled(),
                action,
                actorId
        );
    }
}
