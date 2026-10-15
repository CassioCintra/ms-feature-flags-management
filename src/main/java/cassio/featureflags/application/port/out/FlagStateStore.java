package cassio.featureflags.application.port.out;

import cassio.featureflags.domain.FeatureFlag;

import java.util.List;

public interface FlagStateStore {

    void save(FeatureFlag flag);

    void remove(FeatureFlag flag);

    void replaceAll(List<FeatureFlag> flags);
}
