package cassio.featureflags.adapter.in.web.response;

import cassio.featureflags.domain.EvaluationResult;
import cassio.featureflags.domain.FlagType;

public record EvaluationResponse(String flagName, boolean enabled, FlagType type, Integer rollout) {

    public static EvaluationResponse from(EvaluationResult result) {
        return new EvaluationResponse(result.flagName(), result.enabled(), result.type(), result.rollout());
    }
}
