package ca.ucalgary.frs.ui.agent;

import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.ui.booking.StatusViews;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.components.ListTableModel;
import ca.ucalgary.frs.ui.components.Tables;
import ca.ucalgary.frs.ui.components.Tables.Labeled;

import javax.swing.JTable;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static ca.ucalgary.frs.ui.components.ListTableModel.column;

/** Schedule columns shared by the agent's schedule and the administrator's flight list. */
public final class FlightColumns {
    private FlightColumns() {
    }

    public static ListTableModel<FlightAvailability> model(Supplier<LocalDateTime> now) {
        return new ListTableModel<>(
                column("Departs", LocalDateTime.class, (FlightAvailability a) -> a.flight().departureTime()),
                column("Arrives", String.class, (FlightAvailability a) -> a.flight().arrivalTime().format(Formats.TIME)),
                column("Flight", String.class, (FlightAvailability a) -> a.flight().flightNumber()),
                column("Airline", String.class, (FlightAvailability a) -> a.flight().airline().name()),
                column("Route", String.class, (FlightAvailability a) -> a.flight().route().label()),
                column("Aircraft", String.class, (FlightAvailability a) -> a.flight().aircraft().model()),
                column("Status", Labeled.class, (FlightAvailability a) -> StatusViews.flight(a.flight(), now.get())),
                column("Booked", String.class, (FlightAvailability a) -> (a.capacity() - a.totalAvailable()) + " / " + a.capacity()),
                column("Load", Integer.class, FlightAvailability::loadFactorPercent),
                column("Base fare", Money.class, (FlightAvailability a) -> a.flight().baseFare()));
    }

    public static void configure(JTable table) {
        Tables.widths(table, 170, 60, 70, 110, 100, 150, 100, 80, 120, 90);
        table.getColumnModel().getColumn(8).setCellRenderer(new Tables.LoadRenderer());
    }

    /** Text used by the quick filter. */
    public static String searchText(FlightAvailability a) {
        var f = a.flight();
        return (f.flightNumber() + " " + f.airline().name() + " " + f.route().origin().code() + " "
                + f.route().destination().code() + " " + f.route().origin().city() + " " + f.route().destination().city()
                + " " + f.aircraft().model()).toLowerCase();
    }
}
