package ca.ucalgary.frs.data.migration;

import ca.ucalgary.frs.data.db.Database;

import java.sql.Statement;
import java.util.Comparator;
import java.util.List;

/**
 * Brings the database up to the latest schema version. Each migration runs in its own transaction
 * together with the {@code user_version} bump, so a failed migration leaves the version unchanged.
 */
public final class SchemaMigrator {
    private final Database db;
    private final List<Migration> migrations;

    public SchemaMigrator(Database db, List<Migration> migrations) {
        this.db = db;
        this.migrations = migrations.stream().sorted(Comparator.comparingInt(Migration::version)).toList();
    }

    /** Returns the schema version after migrating. */
    public int migrate() {
        int current = currentVersion();
        for (Migration m : migrations) {
            if (m.version() <= current) {
                continue;
            }
            db.inTransaction(() -> db.withConnection(c -> {
                m.apply(c);
                try (Statement st = c.createStatement()) {
                    st.execute("PRAGMA user_version = " + m.version());
                }
                return null;
            }));
            current = m.version();
        }
        return current;
    }

    public int currentVersion() {
        return db.withConnection(c -> {
            try (Statement st = c.createStatement(); var rs = st.executeQuery("PRAGMA user_version")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }
}
