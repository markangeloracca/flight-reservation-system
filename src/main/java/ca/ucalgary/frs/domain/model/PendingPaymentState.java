package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.BusinessRuleException;

/** Seat is held while the customer pays. Payment confirms it; abandoning cancels it. */
final class PendingPaymentState implements ReservationState {
    static final PendingPaymentState INSTANCE = new PendingPaymentState();

    private PendingPaymentState() {
    }

    @Override
    public ReservationStatus status() {
        return ReservationStatus.PENDING_PAYMENT;
    }

    @Override
    public boolean holdsSeat() {
        return true;
    }

    @Override
    public void confirm(Reservation reservation) {
        reservation.transitionTo(ConfirmedState.INSTANCE);
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.transitionTo(CancelledState.INSTANCE);
    }

    @Override
    public void checkModifiable(Reservation reservation) {
        throw new BusinessRuleException("Complete payment before changing this reservation.");
    }
}
