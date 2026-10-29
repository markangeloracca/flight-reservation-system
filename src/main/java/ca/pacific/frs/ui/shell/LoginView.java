package ca.pacific.frs.ui.shell;

import ca.pacific.frs.ui.components.*;
import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.Tone;
import ca.pacific.frs.ui.theme.Palette;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Use case: Log in. Also the entry point to customer self-registration. */
final class LoginView extends JPanel {
    private final AppContext ctx;
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final Banner banner = new Banner();
    private final JButton signIn = new JButton("Sign in");

    LoginView(AppContext ctx) {
        super(new MigLayout("insets 0, fill", "[45%, fill][55%, fill]", "[grow, fill]"));
        this.ctx = ctx;
        add(new Hero());
        add(formSide());
    }

    private JPanel formSide() {
        JPanel side = new JPanel(new MigLayout("insets 32, fill", "[center]", "[center]"));
        Card card = new Card(new MigLayout("insets 12 12 8 12, wrap 1, fillx", "[grow, fill]", ""));

        card.add(Typography.h1("Welcome back"));
        card.add(Typography.caption("Sign in to search flights and manage bookings."), "gapbottom 16");
        card.add(banner, "hidemode 3, gapbottom 8");

        username.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Username");
        username.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.muted("user", Icons.SMALL));
        username.getAccessibleContext().setAccessibleName("Username");
        password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Password");
        password.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.muted("lock", Icons.SMALL));
        password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        password.getAccessibleContext().setAccessibleName("Password");
        JLabel userLabel = Typography.strong("Username");
        userLabel.setLabelFor(username);
        userLabel.setDisplayedMnemonic('U');
        JLabel passLabel = Typography.strong("Password");
        passLabel.setLabelFor(password);
        passLabel.setDisplayedMnemonic('P');
        card.add(userLabel);
        card.add(username, "gapbottom 12, h 36!");
        card.add(passLabel);
        card.add(password, "gapbottom 20, h 36!");

        signIn.putClientProperty(FlatClientProperties.STYLE, Toolbar.PRIMARY_STYLE);
        signIn.addActionListener(e -> signIn());
        card.add(signIn, "h 38!");
        username.addActionListener(e -> password.requestFocusInWindow());
        password.addActionListener(e -> signIn());

        JButton register = new JButton("New customer? Create an account");
        register.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        register.putClientProperty(FlatClientProperties.STYLE, "foreground: $Frs.accentColor");
        register.addActionListener(e -> RegisterDialog.show(this, ctx));
        card.add(register, "growx 0, alignx center, gaptop 8");

        card.add(new JSeparator(), "gaptop 12, gapbottom 8");
        card.add(Typography.small("Demo accounts"), "alignx center");
        JPanel demo = new JPanel(new MigLayout("insets 0, gap 6, alignx center"));
        demo.setOpaque(false);
        demo.add(demoButton("Customer", "customer", "customer123"));
        demo.add(demoButton("Agent", "agent", "agent123"));
        demo.add(demoButton("Admin", "admin", "admin123"));
        card.add(demo, "alignx center");

        side.add(card, "w 400!");
        return side;
    }

    private JButton demoButton(String label, String user, String pass) {
        JButton b = new JButton(label);
        b.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 3,12,3,12");
        b.setToolTipText(user + " / " + pass);
        b.addActionListener(e -> {
            username.setText(user);
            password.setText(pass);
            signIn();
        });
        return b;
    }

    private void signIn() {
        banner.hideBanner();
        String user = username.getText();
        char[] pass = password.getPassword();
        signIn.setEnabled(false);
        signIn.setText("Signing in…");
        BackgroundTask.of(() -> ctx.auth().login(user, pass))
                .onSuccess(u -> { })
                .onFailure(err -> {
                    banner.show(Tone.DANGER, UiErrors.message(err));
                    password.setText("");
                    password.requestFocusInWindow();
                })
                .onFinally(() -> {
                    signIn.setEnabled(true);
                    signIn.setText("Sign in");
                })
                .start(this);
    }

    /** Clears the form when returning after sign-out. */
    void reset() {
        password.setText("");
        banner.hideBanner();
        SwingUtilities.invokeLater(() -> (username.getText().isBlank() ? username : password).requestFocusInWindow());
    }

    /** Decorative panel: accent gradient, soft circles and a short product pitch. */
    private static final class Hero extends JPanel {
        Hero() {
            super(new MigLayout("insets 48, wrap 1, filly", "[grow, fill]", "push[]24[]8[]32[][][]push"));
            setOpaque(false);
            add(new Brand(56, () -> Color.WHITE, Palette::accent));
            JLabel title = Typography.display("Flight Reservation System");
            title.setForeground(Color.WHITE);
            add(title, "wmin 0");
            JLabel tag = new WrapLabel(
                    "Search flights, pick your seat and manage every booking in one place.", 460);
            tag.putClientProperty(FlatClientProperties.STYLE_CLASS, "large");
            tag.setForeground(new Color(255, 255, 255, 220));
            add(tag, "wmin 0");
            add(feature("search", "Search by route, date and airline"));
            add(feature("armchair", "Choose your seat on a live cabin map"));
            add(feature("gift", "Monthly promotions applied at checkout"));
        }

        private static JLabel feature(String icon, String text) {
            JLabel l = new JLabel(text, Icons.tinted(icon, Icons.MEDIUM, () -> Color.WHITE), JLabel.LEFT);
            l.setIconTextGap(UIScale.scale(12));
            l.setForeground(Color.WHITE);
            l.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 6, 0));
            return l;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color a = Palette.accent();
                Color b = a.darker().darker();
                g2.setPaint(new GradientPaint(0, 0, a, getWidth(), getHeight(), b));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                int w = getWidth();
                int h = getHeight();
                g2.fillOval(w - w / 2, -h / 5, w, w);
                g2.fillOval(-w / 3, h - h / 3, (int) (w * 0.8), (int) (w * 0.8));
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(w / 3, h / 3, w / 2, w / 2);
            } finally {
                g2.dispose();
            }
        }
    }
}
