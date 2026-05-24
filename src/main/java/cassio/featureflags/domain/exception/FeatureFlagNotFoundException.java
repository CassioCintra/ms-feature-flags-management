package cassio.featureflags.domain.exception;

public class FeatureFlagNotFoundException extends RuntimeException {

    private FeatureFlagNotFoundException(String message) {
        super(message);
    }

    public static FeatureFlagNotFoundException notFound(Long id) {
        return new FeatureFlagNotFoundException("Feature flag not found: " + id);
    }
}
