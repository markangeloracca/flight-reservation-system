package ca.ucalgary.frs.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Payment lifecycle: INITIATED → AUTHORIZED → COMPLETED (→ REFUNDED), or INITIATED → DECLINED.
 * Each status lists the statuses it may move to.
 */
public enum PaymentStatus {
    INITIATED("Initiated"),
    AUTHORIZED("Authorized"),
    DECLINED("Declined"),
    COMPLETED("Completed"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public Set<PaymentStatus> next() {
        return switch (this) {
            case INITIATED -> EnumSet.of(AUTHORIZED, DECLINED);
            case AUTHORIZED -> EnumSet.of(COMPLETED, DECLINED);
            case COMPLETED -> EnumSet.of(REFUNDED);
            case DECLINED, REFUNDED -> EnumSet.noneOf(PaymentStatus.class);
        };
    }

    public boolean canMoveTo(PaymentStatus target) {
        return next().contains(target);
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
