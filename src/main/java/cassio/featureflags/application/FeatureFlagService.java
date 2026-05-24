package cassio.featureflags.application;

import cassio.featureflags.application.port.in.FeatureFlagUseCase;
import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.application.port.out.FlagEventPublisher;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService implements FeatureFlagUseCase {

    private final FeatureFlagRepository repository;
    private final FlagEventPublisher eventPublisher;

    @Override
    @Transactional
    public FeatureFlag create(CreateFlagCommand command) {
        log.debug("Checking existence [flagName={}, service={}, environment={}]",
                command.flagName(), command.serviceName(), command.environmentName());

        if (repository.existsByFlagNameAndServiceNameAndEnvironmentName(
                command.flagName(), command.serviceName(), command.environmentName())) {
            log.warn("Flag already exists [flagName={}, service={}, environment={}]",
                    command.flagName(), command.serviceName(), command.environmentName());
            throw FeatureFlagAlreadyExistsException.alreadyExists(
                    command.flagName(), command.serviceName(), command.environmentName());
        }

        FeatureFlag flag = FeatureFlag.builder()
                .flagName(command.flagName())
                .serviceName(command.serviceName())
                .environmentName(command.environmentName())
                .enabled(false)
                .build();

        FeatureFlag saved = repository.save(flag);
        log.debug("Flag persisted [id={}, flagName={}]", saved.getId(), saved.getFlagName());

        eventPublisher.publish(saved, FlagAction.CREATED);
        return saved;
    }

    @Override
    @Transactional
    public FeatureFlag update(Long id, UpdateFlagCommand command) {
        log.debug("Looking up flag for update [id={}]", id);

        FeatureFlag flag = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Flag not found for update [id={}]", id);
                    return FeatureFlagNotFoundException.notFound(id);
                });

        FeatureFlag updated = repository.save(flag.toggleEnabled(command.enabled()));
        log.debug("Flag toggled [id={}, enabled={}]", updated.getId(), updated.isEnabled());

        eventPublisher.publish(updated, FlagAction.UPDATED);
        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Looking up flag for deletion [id={}]", id);

        FeatureFlag flag = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Flag not found for deletion [id={}]", id);
                    return FeatureFlagNotFoundException.notFound(id);
                });

        repository.delete(flag);
        log.debug("Flag deleted from repository [id={}]", id);

        eventPublisher.publish(flag, FlagAction.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlag> findByServiceAndEnvironment(String serviceName, String environmentName) {
        log.debug("Querying flags [service={}, environment={}]", serviceName, environmentName);
        List<FeatureFlag> flags = repository.findByServiceNameAndEnvironmentName(serviceName, environmentName);
        log.debug("Query result [service={}, environment={}, count={}]", serviceName, environmentName, flags.size());
        return flags;
    }
}
