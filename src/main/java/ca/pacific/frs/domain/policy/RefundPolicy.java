package ca.pacific.frs.domain.policy;

import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Reservation;

import java.time.LocalDateTime;

/** Strategy: how much of the amount paid is returned when a reservation is cancelled. */
public interface RefundPolicy {

    Money refundFor(Reservation reservation, Money amountPaid, LocalDateTime now);

    /** One sentence explaining the rule that applies, shown before the user confirms. */
    String explain(Reservation reservation, LocalDateTime now);
}
