package ca.pacific.frs.control.confirmation;

import ca.pacific.frs.domain.model.BookingConfirmation;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.domain.model.FareQuote;
import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.Payment;
import ca.pacific.frs.domain.model.Reservation;

import static ca.pacific.frs.control.confirmation.ConfirmationFormats.DATE_TIME;
import static ca.pacific.frs.control.confirmation.ConfirmationFormats.TIMESTAMP;

/** Fixed-width text, suitable for saving or printing. */
public final class PlainTextConfirmationRenderer implements ConfirmationRenderer {
    private static final String RULE = "=".repeat(60);
    private static final String THIN = "-".repeat(60);

    @Override
    public String render(BookingConfirmation c) {
        Reservation r = c.reservation();
        Flight f = r.flight();
        Customer p = r.customer();
        FareQuote fare = r.fare();
        StringBuilder sb = new StringBuilder();
        sb.append(RULE).append('\n')
                .append("  FLIGHT RESERVATION SYSTEM - BOOKING CONFIRMATION\n")
                .append(RULE).append('\n')
                .append(row("Booking reference", r.confirmationCode()))
                .append(row("Status", r.status().displayName()))
                .append(row("Issued", c.generatedAt().format(TIMESTAMP)))
                .append('\n').append("PASSENGER\n").append(THIN).append('\n')
                .append(row("Name", p.fullName()))
                .append(row("Email", p.email()))
                .append('\n').append("FLIGHT\n").append(THIN).append('\n')
                .append(row("Flight", f.flightNumber() + " (" + f.airline().name() + ")"))
                .append(row("From", f.route().origin().name() + " (" + f.route().origin().code() + ")"))
                .append(row("To", f.route().destination().name() + " (" + f.route().destination().code() + ")"))
                .append(row("Departs", f.departureTime().format(DATE_TIME)))
                .append(row("Arrives", f.arrivalTime().format(DATE_TIME)))
                .append(row("Duration", ConfirmationFormats.duration(f.duration())))
                .append(row("Aircraft", f.aircraft().model()))
                .append(row("Seat", r.seat().code() + " - " + r.seat().seatClass().displayName()))
                .append('\n').append("FARE\n").append(THIN).append('\n')
                .append(row("Fare", fare.fare().toString()));
        if (fare.hasDiscount()) {
            sb.append(row("Promotion", "-" + fare.discount() + "  " + fare.promotionTitle()));
        }
        sb.append(row("Tax (GST)", fare.tax().toString()))
                .append(row("Total", fare.total().toString()))
                .append(row("Amount paid", c.amountPaid().toString()));
        if (!c.payments().isEmpty()) {
            sb.append('\n').append("PAYMENTS\n").append(THIN).append('\n');
            for (Payment pay : c.payments()) {
                sb.append(String.format("  %-16s %-8s %12s  %-10s %s%n", pay.createdAt().format(TIMESTAMP),
                        pay.type(), pay.amount(), pay.status(), pay.maskedCard()));
            }
        }
        sb.append('\n').append(RULE).append('\n')
                .append("Please arrive at the airport at least 90 minutes before departure.\n")
                .append("This is a simulated booking for the ENSF 614 term project.\n");
        return sb.toString();
    }

    private static String row(String label, String value) {
        return String.format("  %-18s %s%n", label + ":", value);
    }

    @Override
    public String fileExtension() {
        return "txt";
    }

    @Override
    public String formatName() {
        return "Text document";
    }
}
