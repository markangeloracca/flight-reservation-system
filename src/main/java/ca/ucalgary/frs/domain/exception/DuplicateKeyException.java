package ca.ucalgary.frs.domain.exception;

/** A uniqueness rule in storage rejected a write (for example, two live bookings for one seat). */
public class DuplicateKeyException extends PersistenceException {
    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
