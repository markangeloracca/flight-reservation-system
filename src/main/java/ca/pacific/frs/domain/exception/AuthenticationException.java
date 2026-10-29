package ca.pacific.frs.domain.exception;

/** Sign-in failed. Deliberately vague so it does not reveal which part was wrong. */
public class AuthenticationException extends BusinessRuleException {
    public AuthenticationException() {
        super("Incorrect username or password.");
    }
}
