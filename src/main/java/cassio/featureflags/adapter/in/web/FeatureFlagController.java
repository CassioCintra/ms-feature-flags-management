package cassio.featureflags.adapter.in.web;

import cassio.featureflags.adapter.in.web.request.CreateFlagRequest;
import cassio.featureflags.adapter.in.web.request.UpdateFlagRequest;
import cassio.featureflags.adapter.in.web.response.FeatureFlagResponse;
import cassio.featureflags.application.port.in.FeatureFlagUseCase;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.CreateFlagCommand;
import cassio.featureflags.application.port.in.FeatureFlagUseCase.UpdateFlagCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/flags")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagUseCase useCase;

    @PostMapping
    public ResponseEntity<FeatureFlagResponse> create(@RequestBody CreateFlagRequest request) {
        log.info("Creating flag [flagName={}, service={}, environment={}]",
                request.flagName(), request.serviceName(), request.environmentName());
        FeatureFlagResponse response = FeatureFlagResponse.from(
                useCase.create(new CreateFlagCommand(
                        request.flagName(), request.serviceName(), request.environmentName())));
        log.info("Flag created [id={}, flagName={}]", response.id(), response.flagName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeatureFlagResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateFlagRequest request) {
        log.info("Updating flag [id={}, enabled={}]", id, request.enabled());
        FeatureFlagResponse response = FeatureFlagResponse.from(
                useCase.update(id, new UpdateFlagCommand(request.enabled())));
        log.info("Flag updated [id={}, enabled={}]", response.id(), response.enabled());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting flag [id={}]", id);
        useCase.delete(id);
        log.info("Flag deleted [id={}]", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{serviceName}/{environmentName}")
    public ResponseEntity<List<FeatureFlagResponse>> findByServiceAndEnvironment(
            @PathVariable String serviceName,
            @PathVariable String environmentName) {
        log.debug("Fetching flags [service={}, environment={}]", serviceName, environmentName);
        List<FeatureFlagResponse> response = useCase.findByServiceAndEnvironment(serviceName, environmentName)
                .stream()
                .map(FeatureFlagResponse::from)
                .toList();
        log.debug("Flags found [service={}, environment={}, count={}]", serviceName, environmentName, response.size());
        return ResponseEntity.ok(response);
    }
}
