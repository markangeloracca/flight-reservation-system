package ca.ucalgary.frs.domain.model;

/** Final state. The flight has departed with this booking confirmed. */
final class CompletedState implements ReservationState {
    static final CompletedState INSTANCE = new CompletedState();

    private CompletedState() {
    }

    @Override
    public ReservationStatus status() {
        return ReservationStatus.COMPLETED;
    }
}
