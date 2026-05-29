package cassio.featureflags.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@With
public class FeatureFlag {

    private final Long id;
    private final String flagName;
    private final String serviceName;
    private final FlagType type;
    private final Integer rollout;
    private final Map<String, Boolean> environments;
    private final List<String> tags;
    private final String owner;
    private final LocalDate expiresAt;
    private final boolean enabled;

    public FeatureFlag toggleEnabled(boolean enabled) {
        return this.withEnabled(enabled);
    }

    public EvaluationResult evaluate(EvaluationContext context) {
        boolean active = resolveActiveState(context.env());
        if (!active) {
            return new EvaluationResult(flagName, false, effectiveType(), rollout);
        }
        boolean result = switch (effectiveType()) {
            case ROLLOUT -> evaluateRollout(context);
            case BOOLEAN, MULTIVARIATE -> true;
        };
        return new EvaluationResult(flagName, result, effectiveType(), rollout);
    }

    private boolean resolveActiveState(String env) {
        if (!enabled) return false;
        if (env != null && environments != null && !environments.isEmpty()) {
            Boolean envState = environments.get(env);
            return Boolean.TRUE.equals(envState);
        }
        return enabled;
    }

    private FlagType effectiveType() {
        return type != null ? type : FlagType.BOOLEAN;
    }

    private boolean evaluateRollout(EvaluationContext context) {
        if (rollout == null || rollout <= 0) return false;
        if (rollout >= 100) return true;
        int hash = Math.abs((context.userId() + "|" + flagName).hashCode()) % 100;
        return hash < rollout;
    }
}
