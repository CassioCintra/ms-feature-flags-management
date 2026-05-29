package cassio.featureflags.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@With
public class FeatureFlag {

    private final Long id;
    private final String flagName;
    private final String serviceName;
    private final FlagType type;
    private final Integer rollout;
    private final List<String> envs;
    private final List<String> tags;
    private final String owner;
    private final LocalDate expiresAt;
    private final boolean enabled;

    public FeatureFlag toggleEnabled(boolean enabled) {
        return this.withEnabled(enabled);
    }

    public EvaluationResult evaluate(EvaluationContext context) {
        if (!isActiveForEnv(context.env())) {
            return new EvaluationResult(flagName, false, effectiveType(), rollout);
        }
        boolean result = switch (effectiveType()) {
            case ROLLOUT -> evaluateRollout(context);
            case BOOLEAN, MULTIVARIATE -> enabled;
        };
        return new EvaluationResult(flagName, result, effectiveType(), rollout);
    }

    private boolean isActiveForEnv(String env) {
        if (env == null || envs == null || envs.isEmpty()) return true;
        return envs.contains(env);
    }

    private FlagType effectiveType() {
        return type != null ? type : FlagType.BOOLEAN;
    }

    private boolean evaluateRollout(EvaluationContext context) {
        if (!enabled || rollout == null || rollout <= 0) return false;
        if (rollout >= 100) return true;
        int hash = Math.abs((context.userId() + "|" + flagName).hashCode()) % 100;
        return hash < rollout;
    }
}
