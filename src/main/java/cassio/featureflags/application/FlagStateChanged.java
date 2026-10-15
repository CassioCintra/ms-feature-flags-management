package cassio.featureflags.application;

import cassio.featureflags.domain.FeatureFlag;

public record FlagStateChanged(FeatureFlag flag, boolean removed) {}
