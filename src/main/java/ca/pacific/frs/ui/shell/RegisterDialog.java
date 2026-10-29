package ca.pacific.frs.ui.shell;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.control.AuthController;
import ca.pacific.frs.domain.exception.ValidationException;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.FormBuilder;
import ca.pacific.frs.ui.customer.CustomerForm;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;

/** Use case: Register as a customer (creates a profile and a sign-in account, then signs in). */
final class RegisterDialog extends AppDialog {
    private final AppContext ctx;
    private final CustomerForm profile = new CustomerForm();
    private final FormBuilder account = new FormBuilder(2);
    private final JTextField username = new JTextField(16);
    private final JPasswordField password = new JPasswordField(16);
    private final JPasswordField confirm = new JPasswordField(16);
    private final JButton create;

    private RegisterDialog(Component owner, AppContext ctx) {
        super(owner, "Create your account", "Book flights and manage your trips online.");
        this.ctx = ctx;
        password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                AuthController.MIN_PASSWORD_LENGTH + "+ characters, a letter and a digit");
        confirm.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        account.field("username", "Username", username, 2)
                .field("password", "Password", password)
                .field("confirmPassword", "Confirm password", confirm);

        JPanel body = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]"));
        body.add(profile.panel());
        body.add(new JSeparator(), "gapbottom 12");
        body.add(Typography.h3("Sign-in details"), "gapbottom 8");
        body.add(account.panel());
        body.setPreferredSize(new Dimension(UIScale.scale(480), body.getPreferredSize().height));
        setBody(body);
        addSecondaryButton("Cancel", this::dispose);
        create = addPrimaryButton("Create account", this::submit);
    }

    static void show(Component owner, AppContext ctx) {
        new RegisterDialog(owner, ctx).present();
    }

    private void submit() {
        profile.form().clearErrors();
        account.clearErrors();
        Customer c;
        try {
            c = profile.toCustomer();
        } catch (ValidationException e) {
            profile.form().showError(e);
            return;
        }
        String user = username.getText();
        char[] pass = password.getPassword();
        char[] again = confirm.getPassword();
        setBusy(true);
        create.setEnabled(false);
        BackgroundTask.of(() -> ctx.auth().registerCustomer(c, user, pass, again))
                .onSuccess(u -> dispose())
                .onFailure(err -> {
                    if (err instanceof ValidationException ve && ve.field() != null
                            && (ve.field().equals("email") || ve.field().equals("firstName"))) {
                        profile.form().showError(err);
                    } else {
                        account.showError(err);
                    }
                })
                .onFinally(() -> {
                    setBusy(false);
                    create.setEnabled(true);
                })
                .start(this);
    }
}
