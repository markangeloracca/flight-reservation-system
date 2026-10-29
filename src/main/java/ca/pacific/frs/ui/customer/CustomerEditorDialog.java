package ca.pacific.frs.ui.customer;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.domain.exception.ValidationException;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.BackgroundTask;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Optional;

/** Agent dialog for creating or editing a customer profile. */
public final class CustomerEditorDialog extends AppDialog {
    private final AppContext ctx;
    private final CustomerForm form = new CustomerForm();
    private final JButton save;
    private Customer result;

    private CustomerEditorDialog(Component owner, AppContext ctx, Customer existing) {
        super(owner, existing == null ? "New customer" : "Edit customer",
                existing == null ? "Create a profile for a walk-in or phone customer." : existing.fullName());
        this.ctx = ctx;
        form.setCustomer(existing);
        form.panel().setPreferredSize(new Dimension(UIScale.scale(460), form.panel().getPreferredSize().height));
        setBody(form.panel());
        addSecondaryButton("Cancel", this::dispose);
        save = addPrimaryButton(existing == null ? "Create customer" : "Save changes", this::save);
    }

    /** Shows the dialog; returns the saved customer, or empty if cancelled. */
    public static Optional<Customer> show(Component owner, AppContext ctx, Customer existing) {
        CustomerEditorDialog d = new CustomerEditorDialog(owner, ctx, existing);
        d.present();
        return Optional.ofNullable(d.result);
    }

    private void save() {
        form.form().clearErrors();
        Customer c;
        try {
            c = form.toCustomer();
        } catch (ValidationException e) {
            form.form().showError(e);
            return;
        }
        setBusy(true);
        save.setEnabled(false);
        BackgroundTask.of(() -> c.id() == null ? ctx.customers().create(c) : ctx.customers().update(c))
                .onSuccess(saved -> {
                    result = saved;
                    dispose();
                })
                .onFailure(form.form()::showError)
                .onFinally(() -> {
                    setBusy(false);
                    save.setEnabled(true);
                })
                .start(this);
    }
}
