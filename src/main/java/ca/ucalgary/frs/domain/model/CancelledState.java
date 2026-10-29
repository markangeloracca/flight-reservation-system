package ca.ucalgary.frs.domain.model;

/** Final state. The seat has been released. */
final class CancelledState implements ReservationState {
    static final CancelledState INSTANCE = new CancelledState();

    private CancelledState() {
    }

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CANCELLED;
    }
}
