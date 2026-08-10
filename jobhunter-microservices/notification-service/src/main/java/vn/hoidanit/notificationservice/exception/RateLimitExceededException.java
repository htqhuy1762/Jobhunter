package vn.hoidanit.notificationservice.exception;

/**
 * Thrown when a rate-limited operation is invoked too frequently.
 * notification-service has no HTTP controllers - every @RateLimit-annotated
 * method is called from a Kafka consumer - so this stays a plain runtime
 * exception rather than Spring MVC's ResponseStatusException, which implies
 * an HTTP response that doesn't exist in this call path.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
