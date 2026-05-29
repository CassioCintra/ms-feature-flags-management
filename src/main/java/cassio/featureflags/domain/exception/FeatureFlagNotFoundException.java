package cassio.featureflags.domain.exception;

public class FeatureFlagNotFoundException extends RuntimeException {

    private FeatureFlagNotFoundException(String message) {
        super(message);
    }

    public static FeatureFlagNotFoundException notFound(String key) {
        return new FeatureFlagNotFoundException("Feature flag not found: " + key);
    }
}
