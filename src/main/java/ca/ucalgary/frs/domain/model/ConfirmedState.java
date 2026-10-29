package ca.ucalgary.frs.domain.model;

/** Paid and ticketed. Can be changed, cancelled, or completed once the flight departs. */
final class ConfirmedState implements ReservationState {
    static final ConfirmedState INSTANCE = new ConfirmedState();

    private ConfirmedState() {
    }

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public boolean holdsSeat() {
        return true;
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.transitionTo(CancelledState.INSTANCE);
    }

    @Override
    public void checkModifiable(Reservation reservation) {
        // allowed
    }

    @Override
    public void complete(Reservation reservation) {
        reservation.transitionTo(CompletedState.INSTANCE);
    }
}
