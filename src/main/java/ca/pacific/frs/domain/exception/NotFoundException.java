package ca.pacific.frs.domain.exception;

/** A referenced record does not exist (for example, it was deleted in another window). */
public class NotFoundException extends BusinessRuleException {
    public NotFoundException(String what, Object id) {
        super(what + " " + id + " no longer exists.");
    }
}
