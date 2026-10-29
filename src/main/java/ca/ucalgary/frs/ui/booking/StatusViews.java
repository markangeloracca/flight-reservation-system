package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.FlightStatus;
import ca.ucalgary.frs.domain.model.PaymentStatus;
import ca.ucalgary.frs.domain.model.PaymentType;
import ca.ucalgary.frs.domain.model.ReservationStatus;
import ca.ucalgary.frs.ui.components.Tables.Labeled;
import ca.ucalgary.frs.ui.components.Tone;

import java.time.LocalDateTime;

/** Maps domain statuses to coloured chips. */
public final class StatusViews {
    private StatusViews() {
    }

    public static Labeled reservation(ReservationStatus s) {
        return Labeled.of(s.displayName(), switch (s) {
            case PENDING_PAYMENT -> Tone.WARNING;
            case CONFIRMED -> Tone.SUCCESS;
            case CANCELLED -> Tone.DANGER;
            case COMPLETED -> Tone.NEUTRAL;
        });
    }

    public static Labeled flight(Flight f, LocalDateTime now) {
        if (f.status() == FlightStatus.CANCELLED) {
            return Labeled.of("Cancelled", Tone.DANGER);
        }
        if (f.hasDeparted(now)) {
            return Labeled.of("Departed", Tone.NEUTRAL);
        }
        return f.status() == FlightStatus.DELAYED ? Labeled.of("Delayed", Tone.WARNING) : Labeled.of("Scheduled", Tone.ACCENT);
    }

    public static Labeled payment(PaymentType type, PaymentStatus s) {
        Tone tone = switch (s) {
            case COMPLETED -> type == PaymentType.REFUND ? Tone.ACCENT : Tone.SUCCESS;
            case DECLINED -> Tone.DANGER;
            case REFUNDED -> Tone.NEUTRAL;
            default -> Tone.WARNING;
        };
        return Labeled.of(s.displayName(), tone);
    }
}
