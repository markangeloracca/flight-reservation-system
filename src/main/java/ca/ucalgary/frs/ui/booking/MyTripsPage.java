package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.EventBus;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.domain.model.Payment;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.ReservationStatus;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.ContentPane;
import ca.ucalgary.frs.ui.components.EmptyState;
import ca.ucalgary.frs.ui.components.ListTableModel;
import ca.ucalgary.frs.ui.components.Segmented;
import ca.ucalgary.frs.ui.components.Tables;
import ca.ucalgary.frs.ui.components.Tables.Labeled;
import ca.ucalgary.frs.ui.components.Toolbar;
import ca.ucalgary.frs.ui.shell.BasePage;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ca.ucalgary.frs.ui.components.ListTableModel.column;

/** Customer use cases: View reservations and booking history, change or cancel a booking, payment records. */
public final class MyTripsPage extends BasePage {
    enum Filter { UPCOMING, PAST, CANCELLED, ALL }

    private final ListTableModel<Reservation> trips;
    private final JTable tripTable;
    private final ContentPane tripContent;
    private final ListTableModel<Payment> payments;
    private final ContentPane paymentContent;
    private final Segmented<Filter> filter = new Segmented<>(f -> applyFilter());
    private final JButton view;
    private final JButton change;
    private final JButton cancel;
    private final Map<Long, String> codes = new HashMap<>();
    private List<Reservation> all = List.of();
    private LocalDateTime now = LocalDateTime.now();

    public MyTripsPage(AppContext ctx) {
        super(ctx);
        trips = ReservationColumns.model(false);
        tripTable = Tables.create(trips);
        ReservationColumns.widths(tripTable, false);
        Tables.onActivate(tripTable, trips, r -> {
            if (ReservationActions.canViewConfirmation(r)) {
                ReservationActions.viewConfirmation(this, ctx, r);
            }
        });

        view = Toolbar.button("Confirmation", "file-text", () -> selected(r -> ReservationActions.viewConfirmation(this, ctx, r)));
        change = Toolbar.button("Change", "edit", () -> selected(r -> ReservationActions.change(this, ctx, r)));
        cancel = Toolbar.button("Cancel booking", "x-circle", () -> selected(r -> ReservationActions.cancel(this, ctx, r)));
        tripTable.getSelectionModel().addListSelectionListener(e -> updateActions());

        filter.add(Filter.UPCOMING, "Upcoming").add(Filter.PAST, "Past").add(Filter.CANCELLED, "Cancelled")
                .add(Filter.ALL, "All");
        EmptyState emptyTrips = new EmptyState("ticket", "No trips here yet",
                "Bookings you make will appear here, with their confirmation and payment details.");
        tripContent = new ContentPane(Tables.scroll(tripTable), emptyTrips);

        Card tripsCard = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        tripsCard.add(new Toolbar().left(filter).right(view).right(change).right(cancel));
        tripsCard.add(tripContent);

        payments = new ListTableModel<>(
                column("Date", LocalDateTime.class, Payment::createdAt),
                column("Booking", String.class, (Payment p) -> codes.getOrDefault(p.reservationId(), "")),
                column("Type", String.class, (Payment p) -> p.type().toString()),
                column("Amount", Money.class, Payment::amount),
                column("Card", String.class, Payment::maskedCard),
                column("Status", Labeled.class, (Payment p) -> StatusViews.payment(p.type(), p.status())),
                column("Reference", String.class, (Payment p) -> p.transactionRef() == null ? "—" : p.transactionRef()));
        JTable paymentTable = Tables.create(payments);
        Tables.widths(paymentTable, 170, 80, 70, 90, 130, 110, 140);
        paymentContent = new ContentPane(Tables.scroll(paymentTable),
                new EmptyState("credit-card", "No payments yet", "Payments and refunds will be listed here."));
        Card paymentsCard = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        paymentsCard.add(paymentContent);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Trips", tripsCard);
        tabs.addTab("Payment history", paymentsCard);
        add(tabs, "grow, push");

        reloadOn(EventBus.Topic.RESERVATIONS, EventBus.Topic.PAYMENTS);
        updateActions();
    }

    @Override
    public String title() {
        return "My trips";
    }

    @Override
    public String subtitle() {
        return "Your bookings, confirmations and payment records";
    }

    @Override
    public String icon() {
        return "ticket";
    }

    @Override
    protected void reload() {
        record Data(List<Reservation> trips, List<Payment> payments, LocalDateTime now) {
        }
        tripContent.loading();
        paymentContent.loading();
        long customerId = ctx.session().customerId().orElseThrow();
        BackgroundTask.of(() -> new Data(ctx.booking().myReservations(), ctx.customers().payments(customerId),
                        ctx.flightSearch().now()))
                .onSuccess(d -> {
                    now = d.now();
                    all = d.trips();
                    codes.clear();
                    all.forEach(r -> codes.put(r.id(), r.confirmationCode()));
                    payments.setRows(d.payments());
                    paymentContent.showResult(!d.payments().isEmpty());
                    applyFilter();
                })
                .start(this);
    }

    private void applyFilter() {
        Long keep = Tables.selected(tripTable, trips).map(Reservation::id).orElse(null);
        List<Reservation> rows = all.stream().filter(r -> switch (filter.selected()) {
            case UPCOMING -> r.holdsSeat() && !r.flight().hasDeparted(now);
            case PAST -> r.status() == ReservationStatus.COMPLETED
                    || (r.status() == ReservationStatus.CONFIRMED && r.flight().hasDeparted(now));
            case CANCELLED -> r.status() == ReservationStatus.CANCELLED;
            case ALL -> true;
        }).toList();
        if (filter.selected() == Filter.UPCOMING) {
            rows = rows.stream().sorted(java.util.Comparator.comparing(r -> r.flight().departureTime())).toList();
        }
        trips.setRows(rows);
        tripContent.showResult(!rows.isEmpty());
        if (keep != null) {
            Tables.reselect(tripTable, trips, r -> r.id().equals(keep));
        }
        updateActions();
    }

    private void selected(java.util.function.Consumer<Reservation> action) {
        Tables.selected(tripTable, trips).ifPresent(action);
    }

    private void updateActions() {
        Reservation r = Tables.selected(tripTable, trips).orElse(null);
        view.setEnabled(ReservationActions.canViewConfirmation(r));
        change.setEnabled(ReservationActions.canChange(r, now));
        cancel.setEnabled(ReservationActions.canCancel(r, now));
    }
}
