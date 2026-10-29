package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.BusinessRuleException;

/**
 * State pattern: each reservation state decides which lifecycle operations are legal.
 * Default implementations reject the operation; concrete states override what they allow.
 */
public interface ReservationState {

    ReservationStatus status();

    /** Whether a reservation in this state occupies its seat. */
    default boolean holdsSeat() {
        return false;
    }

    default void confirm(Reservation reservation) {
        throw notAllowed("confirmed");
    }

    default void cancel(Reservation reservation) {
        throw notAllowed("cancelled");
    }

    /** Checks that a modification is allowed. Modifying does not change the state. */
    default void checkModifiable(Reservation reservation) {
        throw notAllowed("changed");
    }

    default void complete(Reservation reservation) {
        throw notAllowed("completed");
    }

    private BusinessRuleException notAllowed(String verb) {
        return new BusinessRuleException("A reservation that is " + status().displayName().toLowerCase()
                + " cannot be " + verb + ".");
    }
}
