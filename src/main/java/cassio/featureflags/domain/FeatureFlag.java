package cassio.featureflags.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@With
public class FeatureFlag {

    private final Long id;
    private final String flagName;
    private final String serviceName;
    private final String environmentName;
    private final boolean enabled;

    public FeatureFlag toggleEnabled(boolean enabled) {
        return this.withEnabled(enabled);
    }
}
