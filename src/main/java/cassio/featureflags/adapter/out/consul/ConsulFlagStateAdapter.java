package cassio.featureflags.adapter.out.consul;

import cassio.featureflags.application.port.out.FlagStateStore;
import cassio.featureflags.domain.FeatureFlag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConsulFlagStateAdapter implements FlagStateStore {

    private static final String ROOT = "feature-flags/";
    private static final String READ_POLICY_PREFIX = "flags-read-";

    private final ConsulClient consul;
    private final List<String> environments;
    private final Set<UUID> workspacesWithReadPolicy = ConcurrentHashMap.newKeySet();

    public ConsulFlagStateAdapter(ConsulClient consul, @Value("${feature-flag.environments}") List<String> environments) {
        this.consul = consul;
        this.environments = environments;
    }

    @Override
    public void save(FeatureFlag flag) {
        ensureReadPolicy(flag.getWorkspaceId());
        sync(entriesOf(flag), currentEntriesOf(flag));
    }

    @Override
    public void remove(FeatureFlag flag) {
        sync(Map.of(), currentEntriesOf(flag));
    }

    @Override
    public void replaceAll(List<FeatureFlag> flags) {
        flags.stream().map(FeatureFlag::getWorkspaceId).distinct().forEach(this::ensureReadPolicy);
        var desired = new HashMap<String, String>();
        flags.forEach(flag -> desired.putAll(entriesOf(flag)));
        sync(desired, consul.read(ROOT));
    }

    private void ensureReadPolicy(UUID workspaceId) {
        if (workspacesWithReadPolicy.contains(workspaceId)) return;
        consul.ensurePolicy(READ_POLICY_PREFIX + workspaceId,
                "key_prefix \"" + ROOT + workspaceId + "/\" { policy = \"read\" }");
        workspacesWithReadPolicy.add(workspaceId);
    }

    private void sync(Map<String, String> desired, Map<String, String> current) {
        var sets = new HashMap<String, String>();
        desired.forEach((key, value) -> {
            if (!Objects.equals(value, current.get(key))) sets.put(key, value);
        });
        var deletes = current.keySet().stream().filter(key -> !desired.containsKey(key)).toList();

        if (sets.isEmpty() && deletes.isEmpty()) return;
        consul.write(sets, deletes);
        log.info("Consul flag state synced [written={}, deleted={}]", sets.size(), deletes.size());
    }

    private Map<String, String> currentEntriesOf(FeatureFlag flag) {
        var current = consul.read(prefixOf(flag));
        current.keySet().removeIf(key -> !key.endsWith("/" + flag.getFlagName()));
        return current;
    }

    private Map<String, String> entriesOf(FeatureFlag flag) {
        var flagEnvironments = new TreeSet<>(environments);
        if (flag.getEnvironments() != null) flagEnvironments.addAll(flag.getEnvironments().keySet());

        var entries = new HashMap<String, String>();
        for (var env : flagEnvironments) {
            var key = prefixOf(flag) + env + "/" + flag.getFlagName();
            entries.put(key, "{\"enabled\":" + flag.resolveActiveState(env) + "}");
        }
        return entries;
    }

    private static String prefixOf(FeatureFlag flag) {
        return ROOT + flag.getWorkspaceId() + "/" + flag.getServiceName() + "/";
    }
}
