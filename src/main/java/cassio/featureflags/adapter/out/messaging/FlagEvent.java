package cassio.featureflags.adapter.out.messaging;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;

public record FlagEvent(
        String flagName,
        String serviceName,
        String environmentName,
        boolean enabled,
        FlagAction action
) {
    public static FlagEvent from(FeatureFlag flag, FlagAction action) {
        return new FlagEvent(
                flag.getFlagName(),
                flag.getServiceName(),
                flag.getEnvironmentName(),
                flag.isEnabled(),
                action
        );
    }
}
