package ca.ucalgary.frs.data.db;

import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Opens file-backed SQLite connections with the project's pragma policy:
 * WAL journaling, NORMAL sync, enforced foreign keys, and a 5 s busy timeout.
 */
public final class SqliteConnectionFactory implements ConnectionFactory {
    private final String url;
    private final SQLiteConfig config;

    public SqliteConnectionFactory(Path databaseFile) {
        this.url = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        this.config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5000);
    }

    @Override
    public Connection open() throws SQLException {
        return config.createConnection(url);
    }
}
