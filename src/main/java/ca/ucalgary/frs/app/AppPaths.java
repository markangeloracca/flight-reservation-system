package ca.ucalgary.frs.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Per-user data folder:
 * macOS {@code ~/Library/Application Support/FlightReservationSystem},
 * Windows {@code %APPDATA%\FlightReservationSystem}, Linux {@code ~/.local/share/flight-reservation-system}.
 * Override with {@code -Dfrs.dataDir=/some/path}.
 */
public final class AppPaths {
    private static final String APP = "FlightReservationSystem";

    private final Path dataDir;

    private AppPaths(Path dataDir) {
        this.dataDir = dataDir;
    }

    public static AppPaths resolve() {
        String override = System.getProperty("frs.dataDir");
        Path dir = override != null ? Path.of(override) : defaultDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create data folder " + dir, e);
        }
        return new AppPaths(dir);
    }

    private static Path defaultDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path home = Path.of(System.getProperty("user.home"));
        if (os.contains("mac")) {
            return home.resolve("Library").resolve("Application Support").resolve(APP);
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return (appData != null ? Path.of(appData) : home.resolve("AppData").resolve("Roaming")).resolve(APP);
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        return (xdg != null && !xdg.isBlank() ? Path.of(xdg) : home.resolve(".local").resolve("share"))
                .resolve("flight-reservation-system");
    }

    public Path dataDir() {
        return dataDir;
    }

    public Path databaseFile() {
        return dataDir.resolve("frs.db");
    }

    public Path lockFile() {
        return dataDir.resolve(".lock");
    }
}
