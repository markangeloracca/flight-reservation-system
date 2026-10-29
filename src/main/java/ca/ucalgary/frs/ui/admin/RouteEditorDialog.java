package ca.ucalgary.frs.ui.admin;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Airport;
import ca.ucalgary.frs.domain.model.Route;
import ca.ucalgary.frs.ui.components.AppDialog;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.FormBuilder;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;

/** Administrator dialog to add or edit a route. */
public final class RouteEditorDialog extends AppDialog {
    private final AppContext ctx;
    private final Route existing;
    private final FormBuilder form = new FormBuilder(2);
    private final JComboBox<Airport> origin = new JComboBox<>();
    private final JComboBox<Airport> destination = new JComboBox<>();
    private final JSpinner distance = new JSpinner(new SpinnerNumberModel(500, 50, 20_000, 10));
    private final JButton save;
    private Route result;

    private RouteEditorDialog(Component owner, AppContext ctx, List<Airport> airports, Route existing) {
        super(owner, existing == null ? "Add route" : "Edit route " + existing.label(), null);
        this.ctx = ctx;
        this.existing = existing;
        origin.setModel(new DefaultComboBoxModel<>(airports.toArray(Airport[]::new)));
        destination.setModel(new DefaultComboBoxModel<>(airports.toArray(Airport[]::new)));
        if (existing != null) {
            origin.setSelectedItem(existing.origin());
            destination.setSelectedItem(existing.destination());
            distance.setValue(existing.distanceKm());
        } else if (airports.size() > 1) {
            destination.setSelectedIndex(1);
        }
        form.field("origin", "Origin", origin)
                .field("destination", "Destination", destination)
                .field("distanceKm", "Distance (km)", distance);
        form.panel().setPreferredSize(new Dimension(UIScale.scale(440), form.panel().getPreferredSize().height));
        setBody(form.panel());
        addSecondaryButton("Cancel", this::dispose);
        save = addPrimaryButton(existing == null ? "Add route" : "Save changes", this::save);
    }

    public static void open(Component owner, AppContext ctx, Route existing, java.util.function.Consumer<Route> onSaved) {
        BackgroundTask.of(() -> ctx.routesAndAircraft().airports())
                .onSuccess(airports -> {
                    RouteEditorDialog d = new RouteEditorDialog(owner, ctx, airports, existing);
                    d.present();
                    Optional.ofNullable(d.result).ifPresent(onSaved);
                })
                .start(owner);
    }

    private void save() {
        form.clearErrors();
        Route route;
        try {
            route = new Route(existing == null ? null : existing.id(), (Airport) origin.getSelectedItem(),
                    (Airport) destination.getSelectedItem(), (Integer) distance.getValue());
        } catch (ValidationException e) {
            form.showError(e);
            return;
        }
        setBusy(true);
        save.setEnabled(false);
        BackgroundTask.of(() -> ctx.routesAndAircraft().saveRoute(route))
                .onSuccess(r -> {
                    result = r;
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
