package ca.pacific.frs.data.migration;

import java.sql.Connection;
import java.sql.SQLException;

/** One versioned schema change. Versions are applied in order and tracked in {@code PRAGMA user_version}. */
public interface Migration {
    int version();

    String description();

    void apply(Connection connection) throws SQLException;
}
