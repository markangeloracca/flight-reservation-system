package ca.pacific.frs.domain.exception;

/** The signed-in user is not allowed to perform the requested operation. */
public class AuthorizationException extends BusinessRuleException {
    public AuthorizationException(String message) {
        super(message);
    }
}
