package ca.ucalgary.frs;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.app.AppPaths;
import ca.ucalgary.frs.app.SingleInstanceLock;
import ca.ucalgary.frs.data.db.SqliteConnectionFactory;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.UiErrors;
import ca.ucalgary.frs.ui.shell.MainFrame;
import ca.ucalgary.frs.ui.shell.Splash;
import ca.ucalgary.frs.ui.theme.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point. Opens (and on first run creates and seeds) the SQLite database off the event thread,
 * then shows the main window. Only one copy can run at a time.
 */
public final class FlightReservationApp {
    private static final Logger LOG = Logger.getLogger(FlightReservationApp.class.getName());

    private FlightReservationApp() {
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Flight Reservation System");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Uncaught exception on " + t.getName(), e));
        SwingUtilities.invokeLater(FlightReservationApp::start);
    }

    private static void start() {
        Theme.install();
        AppPaths paths;
        try {
            paths = AppPaths.resolve();
        } catch (RuntimeException e) {
            fatal("The data folder could not be created.\n" + e.getMessage());
            return;
        }
        Optional<SingleInstanceLock> lock = SingleInstanceLock.tryAcquire(paths.lockFile());
        if (lock.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Flight Reservation System is already open.",
                    "Already running", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
            return;
        }

        Splash splash = new Splash();
        splash.setVisible(true);
        BackgroundTask.of(() -> AppContext.production(new SqliteConnectionFactory(paths.databaseFile())))
                .onSuccess(ctx -> {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            ctx.database().optimizeAndCheckpoint();
                        } catch (RuntimeException e) {
                            LOG.log(Level.WARNING, "Could not checkpoint the database on exit", e);
                        } finally {
                            lock.get().close();
                        }
                    }, "frs-shutdown"));
                    BackgroundTask.of(() -> ctx.booking().runHousekeeping()).onFailure(err -> { }).start(null);
                    MainFrame frame = new MainFrame(ctx);
                    splash.dispose();
                    frame.setVisible(true);
                })
                .onFailure(err -> {
                    splash.dispose();
                    lock.get().close();
                    fatal("The database at " + paths.databaseFile() + " could not be opened.\n\n" + UiErrors.message(err));
                })
                .start(null);
    }

    private static void fatal(String message) {
        JOptionPane.showMessageDialog(null, message, "Flight Reservation System", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
