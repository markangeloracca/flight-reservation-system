package ca.pacific.frs.domain.model;

public enum FlightStatus {
    SCHEDULED("Scheduled"),
    DELAYED("Delayed"),
    CANCELLED("Cancelled");

    private final String displayName;

    FlightStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
