package cassio.featureflags.domain;

import java.util.Map;

public record EvaluationContext(String userId, String env, Map<String, String> attributes) {

    public static EvaluationContext of(String userId, String env, Map<String, String> attributes) {
        return new EvaluationContext(userId, env, attributes != null ? attributes : Map.of());
    }
}
