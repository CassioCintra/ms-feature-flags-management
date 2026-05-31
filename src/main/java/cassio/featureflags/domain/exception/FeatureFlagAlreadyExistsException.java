package cassio.featureflags.domain.exception;

public class FeatureFlagAlreadyExistsException extends RuntimeException {

    private FeatureFlagAlreadyExistsException(String message) {
        super(message);
    }

    public static FeatureFlagAlreadyExistsException alreadyExists(String flagName) {
        return new FeatureFlagAlreadyExistsException("Feature flag already exists: " + flagName);
    }
}
