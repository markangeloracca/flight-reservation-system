package ca.ucalgary.frs.domain.exception;

/** Input failed validation (a missing field, a bad format, an out-of-range value). */
public class ValidationException extends BusinessRuleException {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ValidationException(String message) {
        this(null, message);
    }

    /** The name of the offending field, or {@code null} when the error is not tied to one field. */
    public String field() {
        return field;
    }
}
