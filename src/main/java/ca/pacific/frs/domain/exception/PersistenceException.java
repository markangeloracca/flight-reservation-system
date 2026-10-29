package ca.pacific.frs.domain.exception;

/** Storage failed for a technical reason (disk, lock, corrupt file). Not caused by user input. */
public class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
