package ca.pacific.frs.ui.admin;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.domain.exception.ValidationException;
import ca.pacific.frs.domain.model.Aircraft;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.FormBuilder;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Optional;

/** Administrator dialog to add or edit an aircraft and its cabin layout. */
public final class AircraftEditorDialog extends AppDialog {
    private final AppContext ctx;
    private final Aircraft existing;
    private final FormBuilder form = new FormBuilder(3);
    private final JTextField registration = new JTextField(10);
    private final JTextField model = new JTextField(20);
    private final JSpinner business = new JSpinner(new SpinnerNumberModel(3, 0, 20, 1));
    private final JSpinner economy = new JSpinner(new SpinnerNumberModel(24, 1, 50, 1));
    private final JComboBox<Integer> perRow = new JComboBox<>(new Integer[]{6, 4});
    private final JLabel capacity = Typography.caption(" ");
    private final JButton save;
    private Aircraft result;

    private AircraftEditorDialog(Component owner, AppContext ctx, Aircraft existing) {
        super(owner, existing == null ? "Add aircraft" : "Edit " + existing.registration(),
                "Business rows are at the front of the cabin, followed by economy.");
        this.ctx = ctx;
        this.existing = existing;
        registration.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "C-FABC");
        model.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Airbus A320");
        if (existing != null) {
            registration.setText(existing.registration());
            model.setText(existing.model());
            business.setValue(existing.businessRows());
            economy.setValue(existing.economyRows());
            perRow.setSelectedItem(existing.seatsPerRow());
        }
        form.field("registration", "Registration", registration)
                .field("model", "Model", model, 2)
                .field("businessRows", "Business rows", business)
                .field("economyRows", "Economy rows", economy)
                .field("seatsPerRow", "Seats per row", perRow)
                .row(capacity);
        business.addChangeListener(e -> updateCapacity());
        economy.addChangeListener(e -> updateCapacity());
        perRow.addActionListener(e -> updateCapacity());
        updateCapacity();
        form.panel().setPreferredSize(new Dimension(UIScale.scale(480), form.panel().getPreferredSize().height));
        setBody(form.panel());
        addSecondaryButton("Cancel", this::dispose);
        save = addPrimaryButton(existing == null ? "Add aircraft" : "Save changes", this::save);
    }

    public static Optional<Aircraft> show(Component owner, AppContext ctx, Aircraft existing) {
        AircraftEditorDialog d = new AircraftEditorDialog(owner, ctx, existing);
        d.present();
        return Optional.ofNullable(d.result);
    }

    private void updateCapacity() {
        int per = (Integer) perRow.getSelectedItem();
        int b = (Integer) business.getValue() * per;
        int e = (Integer) economy.getValue() * per;
        capacity.setText("Capacity " + (b + e) + " seats: " + b + " business, " + e + " economy");
    }

    private void save() {
        form.clearErrors();
        Aircraft a;
        try {
            a = new Aircraft(existing == null ? null : existing.id(), registration.getText(), model.getText(),
                    (Integer) business.getValue(), (Integer) economy.getValue(), (Integer) perRow.getSelectedItem());
        } catch (ValidationException e) {
            form.showError(e);
            return;
        }
        setBusy(true);
        save.setEnabled(false);
        BackgroundTask.of(() -> ctx.routesAndAircraft().saveAircraft(a))
                .onSuccess(saved -> {
                    result = saved;
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
