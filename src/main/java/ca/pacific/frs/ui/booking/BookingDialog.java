package ca.pacific.frs.ui.booking;

import ca.pacific.frs.control.BookingController;
import ca.pacific.frs.ui.components.Formats;
import ca.pacific.frs.ui.components.UiErrors;
import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.Seat;
import ca.pacific.frs.domain.model.SeatClass;
import ca.pacific.frs.domain.model.SeatMap;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.Card;
import ca.pacific.frs.ui.components.Icons;
import ca.pacific.frs.ui.components.Toasts;
import ca.pacific.frs.ui.customer.CustomerEditorDialog;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;

/**
 * Book a flight: choose the passenger (agents) and a seat, see the fare, hold the seat, then pay.
 */
public final class BookingDialog extends AppDialog {
    private final AppContext ctx;
    private final Flight flight;
    private final Optional<Long> selfCustomerId;

    private final SeatMapPanel seatMap = new SeatMapPanel();
    private final FareBreakdown fare = new FareBreakdown();
    private final JLabel seatLabel = Typography.h3("No seat selected");
    private final JLabel availability = Typography.caption(" ");
    private final JComboBox<Customer> customerBox = new JComboBox<>();
    private final JButton next;

    private BookingDialog(Component owner, AppContext ctx, Flight flight) {
        super(owner, "Book " + flight.flightNumber(),
                flight.route().longLabel() + " · " + flight.departureTime().format(
                        Formats.DATE));
        this.ctx = ctx;
        this.flight = flight;
        this.selfCustomerId = ctx.session().customerId();

        JPanel body = new JPanel(new MigLayout("insets 0, fill, gap 16", "[340!][grow, fill]", "[grow, fill]"));
        body.add(leftColumn(), "growy");
        body.add(seatColumn());
        setBody(body);

        addSecondaryButton("Cancel", this::dispose);
        next = addPrimaryButton("Continue to payment", this::holdAndPay);
        next.setIcon(Icons.tinted("arrow-right", Icons.SMALL, () -> next.getForeground()));
        next.setHorizontalTextPosition(JButton.LEFT);
        next.setEnabled(false);

        seatMap.addPropertyChangeListener(SeatMapPanel.PROPERTY_SELECTED, e -> onSeatChanged((Seat) e.getNewValue()));
        customerBox.addActionListener(e -> updateNext());
        loadSeatMap();
        if (selfCustomerId.isEmpty()) {
            loadCustomers(null);
        }
    }

    public static void open(Component owner, AppContext ctx, Flight flight) {
        new BookingDialog(owner, ctx, flight).present();
    }

    private JPanel leftColumn() {
        JPanel col = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 0 12", "[grow, fill]"));
        col.setOpaque(false);
        col.add(new FlightSummary(flight));

        Card passenger = new Card(new MigLayout("insets 0, fillx, wrap 1, gap 0 6", "[grow, fill]"));
        passenger.add(Typography.strong("Passenger"));
        if (selfCustomerId.isPresent()) {
            JLabel me = new JLabel(ctx.session().requireUser().fullName(), Icons.muted("user", Icons.SMALL), JLabel.LEFT);
            passenger.add(me);
        } else {
            customerBox.getAccessibleContext().setAccessibleName("Passenger");
            customerBox.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Choose a customer");
            JButton add = new JButton("New customer", Icons.get("plus"));
            add.addActionListener(e -> CustomerEditorDialog.show(this, ctx, null).ifPresent(this::loadCustomers));
            passenger.add(customerBox);
            passenger.add(add, "growx 0, alignx left");
        }
        col.add(passenger);

        Card price = new Card(new MigLayout("insets 0, fillx, wrap 1, gap 0 8", "[grow, fill]"));
        price.add(seatLabel);
        price.add(fare);
        col.add(price);
        return col;
    }

    private JPanel seatColumn() {
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][][grow, fill]"));
        card.add(Typography.strong("Choose a seat"));
        card.add(availability);
        JPanel wrap = new JPanel(new MigLayout("insets 8, alignx center"));
        wrap.setOpaque(false);
        wrap.add(seatMap);
        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(UIScale.scale(16));
        scroll.setPreferredSize(new Dimension(UIScale.scale(360), UIScale.scale(460)));
        card.add(scroll);
        card.add(SeatMapPanel.legend(), "growx 0");
        return card;
    }

    private void loadSeatMap() {
        BackgroundTask.of(() -> ctx.flightSearch().seatMap(flight.id()))
                .onSuccess(this::showSeatMap)
                .start(this);
    }

    private void showSeatMap(SeatMap map) {
        seatMap.setSeatMap(map, null);
        availability.setText(map.availableCount(SeatClass.ECONOMY) + " economy and "
                + map.availableCount(SeatClass.BUSINESS) + " business seats available");
        pack();
    }

    private void loadCustomers(Customer select) {
        BackgroundTask.of(() -> ctx.customers().search(null))
                .onSuccess((List<Customer> list) -> {
                    customerBox.setModel(new DefaultComboBoxModel<>(list.toArray(Customer[]::new)));
                    customerBox.setSelectedItem(select);
                    if (select == null) {
                        customerBox.setSelectedIndex(-1);
                    }
                    updateNext();
                })
                .start(this);
    }

    private void onSeatChanged(Seat seat) {
        if (seat == null) {
            seatLabel.setText("No seat selected");
            fare.show(null);
            updateNext();
            return;
        }
        seatLabel.setText("Seat " + seat.code() + " · " + seat.seatClass().displayName());
        BackgroundTask.of(() -> ctx.booking().quote(flight, seat.seatClass()))
                .onSuccess(q -> {
                    fare.show(q);
                    updateNext();
                })
                .start(this);
    }

    private void updateNext() {
        boolean hasCustomer = selfCustomerId.isPresent() || customerBox.getSelectedItem() != null;
        next.setEnabled(seatMap.selectedSeat() != null && hasCustomer && !isBusy());
    }

    private long customerId() {
        return selfCustomerId.orElseGet(() -> ((Customer) customerBox.getSelectedItem()).id());
    }

    private String passengerName() {
        return selfCustomerId.isPresent() ? ctx.session().requireUser().fullName()
                : ((Customer) customerBox.getSelectedItem()).fullName();
    }

    /** Holds the seat, then opens payment. Abandoning payment releases the hold. */
    private void holdAndPay() {
        Seat seat = seatMap.selectedSeat();
        setBusy(true);
        next.setEnabled(false);
        BackgroundTask.of(() -> ctx.booking().holdSeat(customerId(), flight.id(), seat.code()))
                .onSuccess(this::pay)
                .onFailure(err -> {
                    UiErrors.show(this, err);
                    loadSeatMap();
                })
                .onFinally(() -> {
                    setBusy(false);
                    updateNext();
                })
                .start(this);
    }

    private void pay(Reservation hold) {
        Optional<Reservation> paid = PaymentDialog.show(this, "Payment",
                "Booking " + hold.confirmationCode() + " · seat " + hold.seat().code() + " is held for "
                        + BookingController.HOLD_EXPIRY.toMinutes() + " minutes.",
                hold.fare().total(), passengerName(), card -> ctx.booking().pay(hold.id(), card));
        if (paid.isPresent()) {
            dispose();
            ConfirmationDialog.open(getOwner(), ctx, hold.id(), true);
        } else {
            BackgroundTask.run(() -> ctx.booking().releaseHold(hold.id()))
                    .onSuccess(v -> {
                        Toasts.info(this, "Payment cancelled. Seat " + hold.seat().code() + " was released.");
                        loadSeatMap();
                    })
                    .start(this);
        }
    }
}
