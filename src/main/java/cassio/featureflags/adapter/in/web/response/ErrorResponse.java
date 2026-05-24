package cassio.featureflags.adapter.in.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Error response following RFC 9457 — Problem Details for HTTP APIs.
 *
 * <pre>
 * {
 *   "type":     "https://problems.feature-flags.io/not-found",
 *   "title":    "Not Found",
 *   "status":   404,
 *   "detail":   "Feature flag not found: 42",
 *   "instance": "/flags/42",
 *   "timestamp": "2025-05-21T14:00:00Z"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        List<FieldError> errors
) {
    private static final String BASE_TYPE_URI = "https://problems.feature-flags.io/";

    public static ErrorResponse of(HttpStatus httpStatus, String detail, String instance) {
        return new ErrorResponse(
                BASE_TYPE_URI + httpStatus.name().toLowerCase().replace('_', '-'),
                httpStatus.getReasonPhrase(),
                httpStatus.value(),
                detail,
                instance,
                Instant.now(),
                null
        );
    }

    public static ErrorResponse withFieldErrors(HttpStatus httpStatus, String detail, String instance, List<FieldError> errors) {
        return new ErrorResponse(
                BASE_TYPE_URI + httpStatus.name().toLowerCase().replace('_', '-'),
                httpStatus.getReasonPhrase(),
                httpStatus.value(),
                detail,
                instance,
                Instant.now(),
                errors
        );
    }

    public record FieldError(String field, String message) {}
}
