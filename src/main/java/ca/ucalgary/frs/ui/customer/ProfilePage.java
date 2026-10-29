package ca.ucalgary.frs.ui.customer;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.EventBus;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.Toasts;
import ca.ucalgary.frs.ui.components.Toolbar;
import ca.ucalgary.frs.ui.shell.BasePage;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;

/** Customer use case: View and edit my details. */
public final class ProfilePage extends BasePage {
    private final CustomerForm form = new CustomerForm();
    private final JButton save;

    public ProfilePage(AppContext ctx) {
        super(ctx);
        save = Toolbar.primary("Save changes", null, this::save);
        Card card = new Card(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]"));
        card.add(Typography.h3("Personal details"));
        card.add(Typography.caption("Signed in as " + ctx.session().requireUser().username()
                + ". These details appear on your booking confirmations."), "gapbottom 12");
        card.add(form.panel());
        card.add(save, "growx 0, alignx right");
        add(card, "w 560!, aligny top");
        reloadOn(EventBus.Topic.CUSTOMERS);
    }

    @Override
    public String title() {
        return "My profile";
    }

    @Override
    public String subtitle() {
        return "Keep your contact details up to date";
    }

    @Override
    public String icon() {
        return "user";
    }

    @Override
    protected void reload() {
        BackgroundTask.of(() -> ctx.customers().myProfile()).onSuccess(form::setCustomer).start(this);
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
        save.setEnabled(false);
        BackgroundTask.of(() -> ctx.customers().update(c))
                .onSuccess(saved -> {
                    form.setCustomer(saved);
                    Toasts.success(this, "Your details were saved.");
                })
                .onFailure(form.form()::showError)
                .onFinally(() -> save.setEnabled(true))
                .start(this);
    }
}
