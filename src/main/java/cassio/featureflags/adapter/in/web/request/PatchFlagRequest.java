package cassio.featureflags.adapter.in.web.request;

import cassio.featureflags.domain.FlagType;

import java.time.LocalDate;
import java.util.List;

public record PatchFlagRequest(
        FlagType type,
        Integer rollout,
        List<String> envs,
        List<String> tags,
        String owner,
        LocalDate expiresAt,
        Boolean enabled
) {}
