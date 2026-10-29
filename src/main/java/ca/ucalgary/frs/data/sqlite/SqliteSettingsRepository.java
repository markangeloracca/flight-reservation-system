package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.repository.SettingsRepository;

import java.util.Optional;

public final class SqliteSettingsRepository implements SettingsRepository {
    private final Database db;

    public SqliteSettingsRepository(Database db) {
        this.db = db;
    }

    @Override
    public Optional<String> get(String key) {
        return db.withConnection(c -> Sql.one(c, "SELECT value FROM app_settings WHERE key = ?", rs -> rs.getString(1), key));
    }

    @Override
    public void put(String key, String value) {
        db.withConnection(c -> Sql.update(c, """
                INSERT INTO app_settings (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """, key, value));
    }
}
