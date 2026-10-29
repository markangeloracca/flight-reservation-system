package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.BookingController.ModificationQuote;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.Seat;
import ca.ucalgary.frs.ui.components.AppDialog;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Banner;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.components.ListTableModel;
import ca.ucalgary.frs.ui.components.Tables;
import ca.ucalgary.frs.ui.components.Toasts;
import ca.ucalgary.frs.ui.components.Tone;
import ca.ucalgary.frs.ui.components.UiErrors;
import ca.ucalgary.frs.ui.theme.Typography;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Objects;

import static ca.ucalgary.frs.ui.components.ListTableModel.column;

/** Change a reservation's seat or move it to another flight on the same route. */
public final class ModifyReservationDialog extends AppDialog {
    private final AppContext ctx;
    private final Reservation reservation;
    private final ListTableModel<FlightAvailability> flights = new ListTableModel<>(
            column("Date", String.class, (FlightAvailability a) -> a.flight().departureTime().format(Formats.SHORT_DATE)),
            column("Departs", String.class, (FlightAvailability a) -> a.flight().departureTime().format(Formats.TIME)),
            column("Flight", String.class, (FlightAvailability a) -> a.flight().flightNumber()),
            column("Seats", Integer.class, FlightAvailability::totalAvailable),
            column("Economy", Money.class, (FlightAvailability a) -> a.flight().fareFor(
                    ca.ucalgary.frs.domain.model.SeatClass.ECONOMY)));
    private final JTable table = Tables.create(flights);
    private final SeatMapPanel seatMap = new SeatMapPanel();
    private final FareBreakdown fare = new FareBreakdown();
    private final Banner banner = new Banner();
    private final JButton confirm;
    private FlightAvailability selectedFlight;
    private ModificationQuote quote;

    private ModifyReservationDialog(Component owner, AppContext ctx, Reservation r) {
        super(owner, "Change reservation " + r.confirmationCode(),
                "Currently " + r.flight().flightNumber() + " on " + Formats.dateTime(r.flight().departureTime())
                        + ", seat " + r.seat().code() + ". Pick a new flight on " + r.flight().route().label()
                        + " and/or a new seat.");
        this.ctx = ctx;
        this.reservation = r;

        Card flightsCard = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        flightsCard.add(Typography.strong("1. Flight"));
        JScrollPane tableScroll = Tables.scroll(table);
        tableScroll.setPreferredSize(new Dimension(UIScale.scale(380), UIScale.scale(380)));
        flightsCard.add(tableScroll);
        Tables.widths(table, 90, 60, 70, 50, 90);

        Card seatCard = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill][]"));
        seatCard.add(Typography.strong("2. Seat"));
        JPanel wrap = new JPanel(new MigLayout("insets 6, alignx center"));
        wrap.setOpaque(false);
        wrap.add(seatMap);
        JScrollPane seatScroll = new JScrollPane(wrap);
        seatScroll.setBorder(null);
        seatScroll.setOpaque(false);
        seatScroll.getViewport().setOpaque(false);
        seatScroll.getVerticalScrollBar().setUnitIncrement(UIScale.scale(16));
        seatScroll.setPreferredSize(new Dimension(UIScale.scale(330), UIScale.scale(380)));
        seatCard.add(seatScroll);
        seatCard.add(SeatMapPanel.legend(), "growx 0");

        Card priceCard = new Card(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]"));
        priceCard.add(Typography.strong("3. New fare"));
        priceCard.add(fare);

        JPanel body = new JPanel(new MigLayout("insets 0, fill, wrap 3, gap 14", "[grow, fill][grow, fill][260!]",
                "[][grow, fill]"));
        body.add(banner, "span, hidemode 3");
        body.add(flightsCard, "grow, aligny top");
        body.add(seatCard, "grow, aligny top");
        body.add(priceCard, "aligny top");
        setBody(body);

        addSecondaryButton("Cancel", this::dispose);
        confirm = addPrimaryButton("Confirm change", this::apply);
        confirm.setEnabled(false);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Tables.selected(table, flights).ifPresent(this::selectFlight);
            }
        });
        seatMap.addPropertyChangeListener(SeatMapPanel.PROPERTY_SELECTED, e -> requote((Seat) e.getNewValue()));
        loadFlights();
    }

    public static void open(Component owner, AppContext ctx, Reservation r) {
        new ModifyReservationDialog(owner, ctx, r).present();
    }

    private void loadFlights() {
        BackgroundTask.of(() -> ctx.flightSearch().alternativesTo(reservation.flight()))
                .onSuccess(list -> {
                    flights.setRows(list);
                    Tables.reselect(table, flights, a -> a.flight().id().equals(reservation.flight().id()));
                    if (table.getSelectedRow() < 0 && !list.isEmpty()) {
                        table.setRowSelectionInterval(0, 0);
                    }
                })
                .start(this);
    }

    private void selectFlight(FlightAvailability a) {
        if (Objects.equals(selectedFlight, a)) {
            return;
        }
        selectedFlight = a;
        quote = null;
        confirm.setEnabled(false);
        fare.show(null);
        boolean sameFlight = a.flight().id().equals(reservation.flight().id());
        BackgroundTask.of(() -> ctx.flightSearch().seatMap(a.flight().id()))
                .onSuccess(map -> seatMap.setSeatMap(map, sameFlight ? reservation.seat().code() : null))
                .start(this);
    }

    private void requote(Seat seat) {
        quote = null;
        confirm.setEnabled(false);
        banner.hideBanner();
        if (seat == null || selectedFlight == null) {
            fare.show(null);
            return;
        }
        long flightId = selectedFlight.flight().id();
        BackgroundTask.of(() -> ctx.booking().quoteModification(reservation.id(), flightId, seat.code()))
                .onSuccess(q -> {
                    quote = q;
                    fare.show(q.newFare());
                    Money diff = q.difference();
                    fare.showExtra(diff.isPositive() ? "You pay" : diff.isNegative() ? "Refund to your card" : "Difference",
                            diff.abs());
                    confirm.setText(diff.isPositive() ? "Pay " + diff + " and confirm" : "Confirm change");
                    confirm.setEnabled(true);
                    pack();
                })
                .onFailure(err -> banner.show(Tone.WARNING, UiErrors.message(err)))
                .start(this);
    }

    private void apply() {
        ModificationQuote q = quote;
        if (q == null) {
            return;
        }
        long id = reservation.id();
        long flightId = q.newFlight().id();
        String seat = q.newSeat().code();
        if (q.requiresPayment()) {
            PaymentDialog.show(this, "Pay fare difference", "Change to " + q.newFlight().flightNumber() + ", seat " + seat,
                    q.difference(), reservation.customer().fullName(),
                    card -> ctx.booking().modify(id, flightId, seat, card))
                    .ifPresent(this::done);
            return;
        }
        setBusy(true);
        confirm.setEnabled(false);
        BackgroundTask.of(() -> ctx.booking().modify(id, flightId, seat, null))
                .onSuccess(this::done)
                .onFailure(err -> banner.show(Tone.WARNING, UiErrors.message(err)))
                .onFinally(() -> {
                    setBusy(false);
                    confirm.setEnabled(quote != null);
                })
                .start(this);
    }

    private void done(Reservation updated) {
        dispose();
        Toasts.success(getOwner(), "Reservation " + updated.confirmationCode() + " now on " + updated.flight().flightNumber()
                + ", seat " + updated.seat().code() + ".");
    }
}
