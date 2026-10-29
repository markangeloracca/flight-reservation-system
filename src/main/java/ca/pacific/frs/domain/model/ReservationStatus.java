package ca.pacific.frs.domain.model;

/** Persisted reservation status. Each value maps to a {@link ReservationState} object (State pattern). */
public enum ReservationStatus {
    PENDING_PAYMENT("Pending payment"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public ReservationState state() {
        return switch (this) {
            case PENDING_PAYMENT -> PendingPaymentState.INSTANCE;
            case CONFIRMED -> ConfirmedState.INSTANCE;
            case CANCELLED -> CancelledState.INSTANCE;
            case COMPLETED -> CompletedState.INSTANCE;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
