package ca.ucalgary.frs.ui.admin;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Aircraft;
import ca.ucalgary.frs.domain.model.Airline;
import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.FlightStatus;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.domain.model.Route;
import ca.ucalgary.frs.ui.components.AppDialog;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.DateField;
import ca.ucalgary.frs.ui.components.FormBuilder;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Administrator dialog to add a flight or update its details and schedule. */
public final class FlightEditorDialog extends AppDialog {
    private final AppContext ctx;
    private final Flight existing;
    private final FormBuilder form = new FormBuilder(2);
    private final JTextField number = new JTextField(8);
    private final JComboBox<Airline> airline = new JComboBox<>();
    private final JComboBox<Route> route = new JComboBox<>();
    private final JComboBox<Aircraft> aircraft = new JComboBox<>();
    private final DateField departure;
    private final DateField arrival;
    private final JTextField fare = new JTextField(8);
    private final JComboBox<FlightStatus> status = new JComboBox<>(new FlightStatus[]{FlightStatus.SCHEDULED, FlightStatus.DELAYED});
    private final JLabel durationHint = Typography.small(" ");
    private final JButton save;
    private Flight result;

    record Lookups(List<Airline> airlines, List<Route> routes, List<Aircraft> aircraft) {
    }

    private FlightEditorDialog(Component owner, AppContext ctx, Lookups lookups, Flight existing) {
        super(owner, existing == null ? "Add flight" : "Edit flight " + existing.flightNumber(),
                existing == null ? "Schedule a new departure." : "Changes are visible to customers immediately.");
        this.ctx = ctx;
        this.existing = existing;
        airline.setModel(new DefaultComboBoxModel<>(lookups.airlines().toArray(Airline[]::new)));
        route.setModel(new DefaultComboBoxModel<>(lookups.routes().toArray(Route[]::new)));
        aircraft.setModel(new DefaultComboBoxModel<>(lookups.aircraft().toArray(Aircraft[]::new)));

        LocalDateTime dep = existing != null ? existing.departureTime()
                : LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
        departure = DateField.dateTime(dep);
        arrival = DateField.dateTime(existing != null ? existing.arrivalTime() : dep.plusHours(2));
        number.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "AC123");
        fare.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "199.00");
        fare.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, new JLabel(" $"));

        if (existing != null) {
            number.setText(existing.flightNumber());
            select(airline, existing.airline(), (a, b) -> a.code().equals(b.code()));
            select(route, existing.route(), (a, b) -> Objects.equals(a.id(), b.id()));
            select(aircraft, existing.aircraft(), (a, b) -> Objects.equals(a.id(), b.id()));
            fare.setText(existing.baseFare().toPlainString());
            status.setSelectedItem(existing.status());
        } else {
            airline.addActionListener(e -> suggestNumber());
            suggestNumber();
        }

        form.field("flightNumber", "Flight number", number)
                .field("airline", "Airline", airline)
                .field("route", "Route", route, 2)
                .field("aircraft", "Aircraft", aircraft, 2)
                .field("departureTime", "Departure (local)", departure)
                .field("arrivalTime", "Arrival (local)", arrival)
                .row(durationHint)
                .field("baseFare", "Base economy fare", fare)
                .field("status", "Status", status);
        departure.addChangeListener(e -> updateDuration());
        arrival.addChangeListener(e -> updateDuration());
        updateDuration();
        form.panel().setPreferredSize(new Dimension(UIScale.scale(500), form.panel().getPreferredSize().height));
        setBody(form.panel());
        addSecondaryButton("Cancel", this::dispose);
        save = addPrimaryButton(existing == null ? "Add flight" : "Save changes", this::save);
    }

    /** Loads airlines, routes and aircraft, then shows the editor. Returns via {@code onSaved}. */
    public static void open(Component owner, AppContext ctx, Flight existing, java.util.function.Consumer<Flight> onSaved) {
        BackgroundTask.of(() -> new Lookups(ctx.flightManagement().airlines(), ctx.flightManagement().routes(),
                        ctx.flightManagement().aircraft()))
                .onSuccess(l -> {
                    FlightEditorDialog d = new FlightEditorDialog(owner, ctx, l, existing);
                    d.present();
                    Optional.ofNullable(d.result).ifPresent(onSaved);
                })
                .start(owner);
    }

    private static <T> void select(JComboBox<T> box, T value, java.util.function.BiPredicate<T, T> same) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (same.test(box.getItemAt(i), value)) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    private void suggestNumber() {
        Airline a = (Airline) airline.getSelectedItem();
        if (a != null && (number.getText().isBlank() || number.getText().length() <= 2)) {
            number.setText(a.code());
        }
    }

    private void updateDuration() {
        Duration d = Duration.between(departure.getDateTime(), arrival.getDateTime());
        durationHint.setText(d.isNegative() || d.isZero() ? "Arrival must be after departure."
                : "Flight time " + Formats.duration(d));
    }

    private void save() {
        form.clearErrors();
        Flight flight;
        try {
            Money base;
            try {
                base = Money.of(fare.getText());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("baseFare", "Enter the fare as a number, for example 199.00.");
            }
            flight = Flight.builder()
                    .id(existing == null ? null : existing.id())
                    .flightNumber(number.getText())
                    .airline((Airline) airline.getSelectedItem())
                    .route((Route) route.getSelectedItem())
                    .aircraft((Aircraft) aircraft.getSelectedItem())
                    .departureTime(departure.getDateTime())
                    .arrivalTime(arrival.getDateTime())
                    .baseFare(base)
                    .status((FlightStatus) status.getSelectedItem())
                    .build();
        } catch (ValidationException e) {
            form.showError(e);
            return;
        }
        setBusy(true);
        save.setEnabled(false);
        BackgroundTask.of(() -> ctx.flightManagement().save(flight))
                .onSuccess(f -> {
                    result = f;
                    dispose();
                })
                .onFailure(form::showError)
                .onFinally(() -> {
                    setBusy(false);
                    save.setEnabled(true);
                })
                .start(this);
    }
}
