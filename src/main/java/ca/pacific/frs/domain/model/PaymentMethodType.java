package ca.pacific.frs.domain.model;

public enum PaymentMethodType {
    CREDIT_CARD("Credit card"),
    DEBIT_CARD("Debit card");

    private final String displayName;

    PaymentMethodType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
