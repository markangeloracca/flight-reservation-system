package ca.ucalgary.frs.control.confirmation;

import ca.ucalgary.frs.domain.model.BookingConfirmation;
import ca.ucalgary.frs.domain.model.FareQuote;
import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.Payment;
import ca.ucalgary.frs.domain.model.Reservation;

import static ca.ucalgary.frs.control.confirmation.ConfirmationFormats.DATE_TIME;
import static ca.ucalgary.frs.control.confirmation.ConfirmationFormats.TIMESTAMP;

/**
 * Simple HTML 3.2 (what Swing's {@code JEditorPane} can display). Colours are passed in so the
 * document matches the current light or dark theme.
 */
public final class HtmlConfirmationRenderer implements ConfirmationRenderer {
    private final String text;
    private final String muted;
    private final String accent;

    public HtmlConfirmationRenderer(String textColor, String mutedColor, String accentColor) {
        this.text = textColor;
        this.muted = mutedColor;
        this.accent = accentColor;
    }

    /** Neutral colours for files saved to disk. */
    public HtmlConfirmationRenderer() {
        this("#1f2328", "#59636e", "#2675BF");
    }

    @Override
    public String render(BookingConfirmation c) {
        Reservation r = c.reservation();
        Flight f = r.flight();
        FareQuote fare = r.fare();
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif;color:" + text + "'>");
        sb.append("<div style='color:").append(muted).append("'>Booking reference</div>")
                .append("<div style='font-size:22pt;font-weight:bold;color:").append(accent).append("'>")
                .append(esc(r.confirmationCode())).append("</div>")
                .append("<div style='color:").append(muted).append("'>").append(esc(r.status().displayName()))
                .append(" · issued ").append(c.generatedAt().format(TIMESTAMP)).append("</div>");

        sb.append("<h3>").append(esc(f.route().origin().city())).append(" (").append(f.route().origin().code())
                .append(") &rarr; ").append(esc(f.route().destination().city())).append(" (")
                .append(f.route().destination().code()).append(")</h3>");

        sb.append("<table cellpadding='3' cellspacing='0'>");
        row(sb, "Passenger", r.customer().fullName());
        row(sb, "Flight", f.flightNumber() + " · " + f.airline().name());
        row(sb, "Departs", f.departureTime().format(DATE_TIME));
        row(sb, "Arrives", f.arrivalTime().format(DATE_TIME));
        row(sb, "Duration", ConfirmationFormats.duration(f.duration()));
        row(sb, "Aircraft", f.aircraft().model());
        row(sb, "Seat", r.seat().code() + " · " + r.seat().seatClass().displayName());
        sb.append("</table>");

        sb.append("<h3>Fare</h3><table cellpadding='3' cellspacing='0'>");
        row(sb, "Fare", fare.fare().toString());
        if (fare.hasDiscount()) {
            row(sb, "Promotion", "−" + fare.discount() + " (" + fare.promotionTitle() + ")");
        }
        row(sb, "Tax (GST)", fare.tax().toString());
        sb.append("<tr><td style='color:").append(muted).append("'><b>Total</b></td><td><b>")
                .append(esc(fare.total().toString())).append("</b></td></tr>");
        row(sb, "Amount paid", c.amountPaid().toString());
        sb.append("</table>");

        if (!c.payments().isEmpty()) {
            sb.append("<h3>Payments</h3><table cellpadding='3' cellspacing='0'>");
            for (Payment p : c.payments()) {
                sb.append("<tr><td style='color:").append(muted).append("'>").append(p.createdAt().format(TIMESTAMP))
                        .append("</td><td>").append(p.type()).append("</td><td>").append(esc(p.amount().toString()))
                        .append("</td><td>").append(p.status()).append("</td><td>").append(esc(p.maskedCard()))
                        .append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append("<p style='color:").append(muted)
                .append("'>Please arrive at the airport at least 90 minutes before departure.</p>");
        return sb.append("</body></html>").toString();
    }

    private void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><td style='color:").append(muted).append("'>").append(esc(label))
                .append("</td><td>").append(esc(value)).append("</td></tr>");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public String fileExtension() {
        return "html";
    }

    @Override
    public String formatName() {
        return "Web page";
    }
}
