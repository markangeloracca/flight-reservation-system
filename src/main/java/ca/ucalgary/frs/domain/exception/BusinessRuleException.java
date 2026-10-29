package ca.ucalgary.frs.domain.exception;

/**
 * A domain rule was violated. The message is written for end users and can be shown in the UI as-is.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
