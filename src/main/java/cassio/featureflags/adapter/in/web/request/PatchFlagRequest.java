package cassio.featureflags.adapter.in.web.request;

import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PatchFlagRequest(
        FlagType type,
        Integer rollout,
        Map<String, Boolean> environments,
        List<String> tags,
        String owner,
        LocalDate expiresAt,
        Boolean enabled
) {}
