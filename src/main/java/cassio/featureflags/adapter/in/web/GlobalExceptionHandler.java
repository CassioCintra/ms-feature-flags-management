package cassio.featureflags.adapter.in.web;

import cassio.featureflags.adapter.in.web.response.ErrorResponse;
import cassio.featureflags.domain.exception.FeatureFlagAlreadyExistsException;
import cassio.featureflags.domain.exception.FeatureFlagNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeatureFlagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(FeatureFlagNotFoundException ex, HttpServletRequest request) {
        log.warn("Flag not found [uri={}, message={}]", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(FeatureFlagAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(FeatureFlagAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Flag conflict [uri={}, message={}]", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Throwable ex, HttpServletRequest request) {
        log.error("Unexpected error [uri={}]", request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request.getRequestURI()));
    }
}
