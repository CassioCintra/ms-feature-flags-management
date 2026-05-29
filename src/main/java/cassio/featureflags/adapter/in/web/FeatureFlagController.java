package cassio.featureflags.adapter.in.web;

import cassio.featureflags.adapter.in.web.request.CreateFlagRequest;
import cassio.featureflags.adapter.in.web.request.EvaluateBatchRequest;
import cassio.featureflags.adapter.in.web.request.PatchFlagRequest;
import cassio.featureflags.adapter.in.web.response.EvaluationResponse;
import cassio.featureflags.adapter.in.web.response.FeatureFlagResponse;
import cassio.featureflags.adapter.in.web.response.ServiceFlagsResponse;
import cassio.featureflags.application.port.in.FeatureFlagUseCase;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.CreateFlagCommand;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.ListFlagsQuery;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.PatchFlagCommand;
import cassio.featureflags.domain.EvaluationContext;
import cassio.featureflags.domain.FlagType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagUseCase useCase;

    @PostMapping("/flags")
    public ResponseEntity<FeatureFlagResponse> create(@RequestBody CreateFlagRequest request) {
        log.info("Creating flag [flagName={}, service={}]", request.flagName(), request.serviceName());
        FeatureFlagResponse response = FeatureFlagResponse.from(
                useCase.create(new CreateFlagCommand(
                        request.flagName(), request.serviceName(),
                        request.type(), request.rollout(),
                        request.envs(), request.tags(),
                        request.owner(), request.expiresAt())));
        log.info("Flag created [flagName={}]", response.flagName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/flags")
    public ResponseEntity<List<FeatureFlagResponse>> listFlags(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) FlagType type,
            @RequestParam(required = false) String q) {
        log.debug("Listing flags [service={}, env={}, type={}, q={}]", service, env, type, q);
        List<FeatureFlagResponse> response = useCase.listFlags(new ListFlagsQuery(service, env, type, q))
                .stream()
                .map(FeatureFlagResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/flags/{key}")
    public ResponseEntity<FeatureFlagResponse> patch(
            @PathVariable String key,
            @RequestBody PatchFlagRequest request) {
        log.info("Patching flag [key={}]", key);
        FeatureFlagResponse response = FeatureFlagResponse.from(
                useCase.patch(key, new PatchFlagCommand(
                        request.type(), request.rollout(),
                        request.envs(), request.tags(),
                        request.owner(), request.expiresAt(),
                        request.enabled())));
        log.info("Flag patched [key={}]", key);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/flags/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        log.info("Deleting flag [key={}]", key);
        useCase.delete(key);
        log.info("Flag deleted [key={}]", key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/flags/{key}/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate(
            @PathVariable String key,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String env,
            @RequestParam Map<String, String> allParams) {
        log.debug("Evaluating flag [key={}, userId={}, env={}]", key, userId, env);
        Map<String, String> attributes = allParams.entrySet().stream()
                .filter(e -> !e.getKey().equals("userId") && !e.getKey().equals("env"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        EvaluationResponse response = EvaluationResponse.from(
                useCase.evaluate(key, EvaluationContext.of(userId, env, attributes)));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/flags/evaluate-batch")
    public ResponseEntity<Map<String, EvaluationResponse>> evaluateBatch(
            @RequestBody EvaluateBatchRequest request) {
        log.debug("Evaluating batch [count={}, userId={}, env={}]",
                request.keys().size(), request.userId(), request.env());
        Map<String, EvaluationResponse> response = useCase.evaluateBatch(
                        request.keys(),
                        EvaluationContext.of(request.userId(), request.env(), request.attributes()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> EvaluationResponse.from(e.getValue())));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceFlagsResponse>> getServices() {
        log.debug("Fetching registered services");
        List<ServiceFlagsResponse> response = useCase.getServices()
                .stream()
                .map(ServiceFlagsResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
