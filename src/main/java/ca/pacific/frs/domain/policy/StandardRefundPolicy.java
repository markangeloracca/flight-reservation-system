package ca.pacific.frs.domain.policy;

import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.ReservationStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Full refund when cancelled at least 24 hours before departure, 50% inside 24 hours.
 * Unpaid holds are refunded in full (there is nothing to keep).
 */
public final class StandardRefundPolicy implements RefundPolicy {
    static final Duration FULL_REFUND_WINDOW = Duration.ofHours(24);
    static final int LATE_REFUND_PERCENT = 50;

    @Override
    public Money refundFor(Reservation reservation, Money amountPaid, LocalDateTime now) {
        if (!amountPaid.isPositive()) {
            return Money.ZERO;
        }
        if (reservation.status() != ReservationStatus.CONFIRMED || isEarly(reservation, now)) {
            return amountPaid;
        }
        return amountPaid.percent(LATE_REFUND_PERCENT);
    }

    @Override
    public String explain(Reservation reservation, LocalDateTime now) {
        return isEarly(reservation, now)
                ? "Cancelling 24 hours or more before departure is fully refunded."
                : "Cancelling within 24 hours of departure refunds " + LATE_REFUND_PERCENT + "% of the amount paid.";
    }

    private boolean isEarly(Reservation reservation, LocalDateTime now) {
        return Duration.between(now, reservation.flight().departureTime()).compareTo(FULL_REFUND_WINDOW) >= 0;
    }
}
