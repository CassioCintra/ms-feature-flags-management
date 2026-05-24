package cassio.featureflags.domain.exception;

public class FeatureFlagAlreadyExistsException extends RuntimeException {

    private FeatureFlagAlreadyExistsException(String message) {
        super(message);
    }

    public static FeatureFlagAlreadyExistsException alreadyExists(String flagName, String serviceName, String environmentName) {
        return new FeatureFlagAlreadyExistsException(
                "Feature flag already exists: " + flagName + " / " + serviceName + " / " + environmentName);
    }
}
