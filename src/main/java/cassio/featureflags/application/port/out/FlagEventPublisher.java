package cassio.featureflags.application.port.out;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;

public interface FlagEventPublisher {

    void publish(FeatureFlag flag, FlagAction action);
}
