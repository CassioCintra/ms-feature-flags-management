package cassio.featureflags.domain;

public record EvaluationResult(String flagName, boolean enabled, FlagType type, Integer rollout) {}
