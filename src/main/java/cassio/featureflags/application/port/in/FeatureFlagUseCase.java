package cassio.featureflags.application.port.in;

import cassio.featureflags.domain.EvaluationContext;
import cassio.featureflags.domain.EvaluationResult;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FeatureFlagUseCase {

    FeatureFlag create(CreateFlagCommand command);

    FeatureFlag patch(String key, PatchFlagCommand command);

    void delete(String key);

    List<FeatureFlag> listFlags(ListFlagsQuery query);

    EvaluationResult evaluate(String key, EvaluationContext context);

    Map<String, EvaluationResult> evaluateBatch(List<String> keys, EvaluationContext context);

    List<ServiceInfo> getServices();

    record CreateFlagCommand(
            String flagName,
            String serviceName,
            FlagType type,
            Integer rollout,
            List<String> envs,
            List<String> tags,
            String owner,
            LocalDate expiresAt
    ) {}

    record PatchFlagCommand(
            FlagType type,
            Integer rollout,
            List<String> envs,
            List<String> tags,
            String owner,
            LocalDate expiresAt,
            Boolean enabled
    ) {}

    record ListFlagsQuery(String service, String env, FlagType type, String search) {}

    record ServiceInfo(String serviceName, List<FeatureFlag> flags) {}
}
