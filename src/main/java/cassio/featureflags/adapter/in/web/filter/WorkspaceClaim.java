package cassio.featureflags.adapter.in.web.filter;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

public final class WorkspaceClaim {

    public static final String NAME = "workspace_id";

    private WorkspaceClaim() {}

    public static Optional<UUID> of(Jwt jwt) {
        var value = jwt.getClaimAsString(NAME);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
