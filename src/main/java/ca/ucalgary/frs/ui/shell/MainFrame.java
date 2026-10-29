package ca.ucalgary.frs.ui.shell;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.Session;
import ca.ucalgary.frs.domain.model.Promotion;
import ca.ucalgary.frs.domain.model.User;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Toasts;
import ca.ucalgary.frs.ui.theme.Theme;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.CardLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * The application window. Shows the sign-in screen or the signed-in workspace, reacting to
 * {@link Session} changes (Observer). Menu items are {@link Action} objects (Command), shared with
 * keyboard shortcuts.
 */
public final class MainFrame extends JFrame {
    private static final int HOUSEKEEPING_MS = 60_000;

    private final AppContext ctx;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginView login;
    private ShellPanel shell;

    private final Action signOut;
    private final Action find;
    private final List<Action> goTo = new ArrayList<>();
    private final JMenu goMenu = new JMenu("Go");

    public MainFrame(AppContext ctx) {
        super("Flight Reservation System");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(appIcons());

        login = new LoginView(ctx);
        root.add(login, "login");
        setContentPane(root);

        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        signOut = action("Sign out", KeyStroke.getKeyStroke(KeyEvent.VK_L, menuKey | InputEvent.SHIFT_DOWN_MASK),
                () -> ctx.auth().logout());
        find = action("Find", KeyStroke.getKeyStroke(KeyEvent.VK_F, menuKey), () -> {
            if (shell != null) {
                shell.focusSearch();
            }
        });
        Action toggleTheme = action("Toggle dark mode", KeyStroke.getKeyStroke(KeyEvent.VK_D, menuKey | InputEvent.SHIFT_DOWN_MASK),
                Theme::toggle);
        for (int i = 0; i < 9; i++) {
            int index = i;
            goTo.add(action("Page " + (i + 1), KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, menuKey), () -> {
                if (shell != null) {
                    shell.navigate(index);
                }
            }));
        }
        setJMenuBar(menuBar(toggleTheme));

        ctx.session().addPropertyChangeListener(e -> {
            if (Session.PROPERTY_USER.equals(e.getPropertyName())) {
                SwingUtilities.invokeLater(() -> onUserChanged((User) e.getNewValue()));
            }
        });
        onUserChanged(null);
        restoreBounds();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveBounds();
            }
        });

        Timer housekeeping = new Timer(HOUSEKEEPING_MS, e -> runHousekeeping());
        housekeeping.start();
    }

    private JMenuBar menuBar(Action toggleTheme) {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        file.add(new JMenuItem(signOut));
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        if (!mac) {
            file.addSeparator();
            file.add(new JMenuItem(action("Exit", null, () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)))));
        }
        JMenu view = new JMenu("View");
        view.setMnemonic('V');
        view.add(new JMenuItem(find));
        view.add(new JMenuItem(toggleTheme));
        goMenu.setMnemonic('G');
        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        help.add(new JMenuItem(action("Demo accounts and test cards", null, this::showHelp)));
        bar.add(file);
        bar.add(view);
        bar.add(goMenu);
        bar.add(help);
        return bar;
    }

    private static Action action(String name, KeyStroke key, Runnable run) {
        Action a = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
            }
        };
        if (key != null) {
            a.putValue(Action.ACCELERATOR_KEY, key);
        }
        return a;
    }

    private void onUserChanged(User user) {
        if (shell != null) {
            root.remove(shell);
            shell.dispose();
            shell = null;
        }
        goMenu.removeAll();
        boolean signedIn = user != null;
        signOut.setEnabled(signedIn);
        find.setEnabled(signedIn);
        goMenu.setEnabled(signedIn);
        if (!signedIn) {
            cards.show(root, "login");
            login.reset();
            return;
        }
        shell = new ShellPanel(ctx, user, () -> ctx.auth().logout());
        root.add(shell, "shell");
        for (int i = 0; i < shell.pageCount() && i < goTo.size(); i++) {
            Action a = goTo.get(i);
            a.putValue(Action.NAME, shell.pageTitle(i));
            goMenu.add(new JMenuItem(a));
        }
        cards.show(root, "shell");
        root.revalidate();
        Toasts.success(shell, "Signed in as " + user.fullName() + ".");
        maybeShowPromotion();
    }

    private void maybeShowPromotion() {
        BackgroundTask.of(() -> ctx.promotions().unseenPromotionForCurrentCustomer())
                .onSuccess((Optional<Promotion> p) -> p.ifPresent(promo -> {
                    PromotionDialog.show(this, promo, YearMonth.now());
                    BackgroundTask.run(() -> ctx.promotions().markSeenByCurrentCustomer()).start(this);
                }))
                .start(this);
    }

    private void runHousekeeping() {
        BackgroundTask.of(() -> ctx.booking().runHousekeeping())
                .onFailure(err -> { })
                .start(this);
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this, """
                <html><b>Demo accounts</b><br>
                customer / customer123 &nbsp;·&nbsp; agent / agent123 &nbsp;·&nbsp; admin / admin123<br><br>
                <b>Test cards</b> (payments are simulated)<br>
                4242 4242 4242 4242: approved<br>
                4000 0000 0000 0002: declined, insufficient funds<br>
                Any future expiry and any 3-digit CVV.<br><br>
                <b>Shortcuts</b><br>
                Ctrl/⌘ + 1…9: switch page · Ctrl/⌘ + F: search · Ctrl/⌘ + Shift + D: dark mode<br>
                Enter: open the selected row · Esc: close a dialog</html>
                """, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private static List<java.awt.Image> appIcons() {
        List<java.awt.Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 32, 64, 128}) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new java.awt.Color(0x2675BF));
            g.fillRoundRect(0, 0, size, size, size / 2, size / 2);
            com.formdev.flatlaf.extras.FlatSVGIcon plane = new com.formdev.flatlaf.extras.FlatSVGIcon("icons/plane.svg",
                    size * 3 / 5, size * 3 / 5);
            plane.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> java.awt.Color.WHITE));
            plane.paintIcon(null, g, size / 5, size / 5);
            g.dispose();
            icons.add(img);
        }
        return icons;
    }

    private void restoreBounds() {
        Preferences p = Preferences.userNodeForPackage(MainFrame.class);
        int w = p.getInt("w", UIScale.scale(1280));
        int h = p.getInt("h", UIScale.scale(800));
        setMinimumSize(new java.awt.Dimension(UIScale.scale(1024), UIScale.scale(680)));
        setSize(w, h);
        int x = p.getInt("x", Integer.MIN_VALUE);
        int y = p.getInt("y", Integer.MIN_VALUE);
        Rectangle screen = getGraphicsConfiguration().getBounds();
        if (x == Integer.MIN_VALUE || !screen.intersects(new Rectangle(x, y, w, h))) {
            setLocationRelativeTo(null);
        } else {
            setLocation(x, y);
        }
        if (p.getBoolean("maximized", false)) {
            setExtendedState(MAXIMIZED_BOTH);
        }
    }

    private void saveBounds() {
        Preferences p = Preferences.userNodeForPackage(MainFrame.class);
        boolean max = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        p.putBoolean("maximized", max);
        if (!max) {
            p.putInt("x", getX());
            p.putInt("y", getY());
            p.putInt("w", getWidth());
            p.putInt("h", getHeight());
        }
    }
}
