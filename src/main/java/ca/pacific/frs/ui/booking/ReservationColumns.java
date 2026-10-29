package ca.pacific.frs.ui.booking;

import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.ui.components.ListTableModel;
import ca.pacific.frs.ui.components.Tables;
import ca.pacific.frs.ui.components.Tables.Labeled;

import javax.swing.JTable;
import java.time.LocalDateTime;

import static ca.pacific.frs.ui.components.ListTableModel.column;

/** Column sets for reservation tables. Agents also see the customer. */
public final class ReservationColumns {
    private ReservationColumns() {
    }

    public static ListTableModel<Reservation> model(boolean withCustomer) {
        if (withCustomer) {
            return new ListTableModel<>(
                    column("Reference", String.class, Reservation::confirmationCode),
                    column("Customer", String.class, (Reservation r) -> r.customer().fullName()),
                    column("Flight", String.class, (Reservation r) -> r.flight().flightNumber()),
                    column("Route", String.class, (Reservation r) -> r.flight().route().label()),
                    column("Departs", LocalDateTime.class, (Reservation r) -> r.flight().departureTime()),
                    column("Seat", String.class, (Reservation r) -> r.seat().code() + " · " + r.seat().seatClass()),
                    column("Status", Labeled.class, (Reservation r) -> StatusViews.reservation(r.status())),
                    column("Total", Money.class, (Reservation r) -> r.fare().total()));
        }
        return new ListTableModel<>(
                column("Reference", String.class, Reservation::confirmationCode),
                column("Flight", String.class, (Reservation r) -> r.flight().flightNumber()),
                column("Route", String.class, (Reservation r) -> r.flight().route().longLabel()),
                column("Departs", LocalDateTime.class, (Reservation r) -> r.flight().departureTime()),
                column("Seat", String.class, (Reservation r) -> r.seat().code() + " · " + r.seat().seatClass()),
                column("Status", Labeled.class, (Reservation r) -> StatusViews.reservation(r.status())),
                column("Total", Money.class, (Reservation r) -> r.fare().total()));
    }

    /** Narrow set for side panels. */
    public static ListTableModel<Reservation> compact() {
        return new ListTableModel<>(
                column("Reference", String.class, Reservation::confirmationCode),
                column("Route", String.class, (Reservation r) -> r.flight().route().label()),
                column("Departs", LocalDateTime.class, (Reservation r) -> r.flight().departureTime()),
                column("Status", Labeled.class, (Reservation r) -> StatusViews.reservation(r.status())),
                column("Total", Money.class, (Reservation r) -> r.fare().total()));
    }

    public static void widths(JTable table, boolean withCustomer) {
        if (withCustomer) {
            Tables.widths(table, 90, 150, 70, 100, 170, 110, 130, 90);
        } else {
            Tables.widths(table, 90, 70, 190, 170, 110, 130, 90);
        }
    }
}
