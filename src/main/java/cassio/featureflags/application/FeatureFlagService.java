package cassio.featureflags.application;

import cassio.featureflags.application.port.in.FeatureFlagUseCase;
import cassio.featureflags.application.port.out.FeatureFlagRepository;
import cassio.featureflags.application.port.out.FlagEventPublisher;
import cassio.featureflags.domain.EvaluationContext;
import cassio.featureflags.domain.EvaluationResult;
import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagAction;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService implements FeatureFlagUseCase {

    private final FeatureFlagRepository repository;
    private final FlagEventPublisher eventPublisher;

    @Override
    @Transactional
    public FeatureFlag create(CreateFlagCommand command) {
        log.debug("Checking existence [flagName={}]", command.flagName());

        if (repository.existsByFlagName(command.flagName())) {
            log.warn("Flag already exists [flagName={}]", command.flagName());
            throw FeatureFlagAlreadyExistsException.alreadyExists(command.flagName());
        }

        var flag = FeatureFlag.builder()
                .flagName(command.flagName())
                .serviceName(command.serviceName())
                .type(command.type())
                .rollout(command.rollout())
                .envs(command.envs())
                .tags(command.tags())
                .owner(command.owner())
                .expiresAt(command.expiresAt())
                .enabled(false)
                .build();

        var persistedFlag = repository.save(flag);
        log.debug("Flag persisted [id={}, flagName={}]", persistedFlag.getId(), persistedFlag.getFlagName());

        eventPublisher.publish(persistedFlag, FlagAction.CREATED);
        return persistedFlag;
    }

    @Override
    @Transactional
    public FeatureFlag patch(String key, PatchFlagCommand command) {
        log.debug("Looking up flag for patch [key={}]", key);

        var flag = repository.findByFlagName(key)
                .orElseThrow(() -> {
                    log.warn("Flag not found for patch [key={}]", key);
                    return FeatureFlagNotFoundException.notFound(key);
                });

        boolean enabledChanged = command.enabled() != null && command.enabled() != flag.isEnabled();

        var builder = patchFlagFields(command, flag);

        var updatedFlag = repository.save(builder.build());
        log.debug("Flag patched [key={}, enabledChanged={}]", key, enabledChanged);

        eventPublisher.publish(updatedFlag, enabledChanged ? FlagAction.TOGGLED : FlagAction.UPDATED);
        return updatedFlag;
    }

    private static FeatureFlag.FeatureFlagBuilder patchFlagFields(PatchFlagCommand command, FeatureFlag flag) {
        FeatureFlag.FeatureFlagBuilder builder = flag.toBuilder();
        if (command.type() != null)      builder.type(command.type());
        if (command.rollout() != null)   builder.rollout(command.rollout());
        if (command.envs() != null)      builder.envs(command.envs());
        if (command.tags() != null)      builder.tags(command.tags());
        if (command.owner() != null)     builder.owner(command.owner());
        if (command.expiresAt() != null) builder.expiresAt(command.expiresAt());
        if (command.enabled() != null)   builder.enabled(command.enabled());
        return builder;
    }

    @Override
    @Transactional
    public void delete(String key) {
        log.debug("Looking up flag for deletion [key={}]", key);

        var flag = repository.findByFlagName(key)
                .orElseThrow(() -> {
                    log.warn("Flag not found for deletion [key={}]", key);
                    return FeatureFlagNotFoundException.notFound(key);
                });

        repository.delete(flag);
        log.debug("Flag deleted [key={}]", key);

        eventPublisher.publish(flag, FlagAction.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlag> listFlags(ListFlagsQuery query) {
        log.debug("Listing flags [service={}, env={}, type={}, search={}]",
                query.service(), query.env(), query.type(), query.search());
        return repository.findAll(query.service(), query.env(), query.type(), query.search());
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResult evaluate(String key, EvaluationContext context) {
        log.debug("Evaluating flag [key={}, userId={}, env={}]",
                key, context.userId(), context.env());

        var flag = repository.findByFlagName(key)
                .orElseThrow(() -> FeatureFlagNotFoundException.notFound(key));

        return flag.evaluate(context);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, EvaluationResult> evaluateBatch(List<String> keys, EvaluationContext context) {
        log.debug("Evaluating batch [count={}, userId={}, env={}]",
                keys.size(), context.userId(), context.env());

        return repository.findByFlagNameIn(keys).stream()
                .collect(Collectors.toMap(
                        FeatureFlag::getFlagName,
                        flag -> flag.evaluate(context)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceInfo> getServices() {
        return repository.findDistinctServiceNames().stream()
                .map(svc -> new ServiceInfo(svc, repository.findAll(svc, null, null, null)))
                .toList();
    }
}
