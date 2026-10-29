package ca.pacific.frs.domain.model;

public enum PaymentType {
    CHARGE("Charge"),
    REFUND("Refund");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
