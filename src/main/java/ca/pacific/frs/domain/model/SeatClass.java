package ca.pacific.frs.domain.model;

import java.math.BigDecimal;

/** Cabin class. The fare multiplier is applied to a flight's base (economy) fare. */
public enum SeatClass {
    ECONOMY("Economy", new BigDecimal("1.00")),
    BUSINESS("Business", new BigDecimal("2.50"));

    private final String displayName;
    private final BigDecimal fareMultiplier;

    SeatClass(String displayName, BigDecimal fareMultiplier) {
        this.displayName = displayName;
        this.fareMultiplier = fareMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public BigDecimal fareMultiplier() {
        return fareMultiplier;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
