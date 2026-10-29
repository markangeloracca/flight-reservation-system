package ca.pacific.frs.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/** Everything printed on a booking confirmation. */
public record BookingConfirmation(Reservation reservation, List<Payment> payments, Money amountPaid,
                                  LocalDateTime generatedAt) {
    public BookingConfirmation {
        payments = List.copyOf(payments);
    }
}
