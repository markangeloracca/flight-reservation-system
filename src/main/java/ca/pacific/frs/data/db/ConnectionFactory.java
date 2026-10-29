package ca.pacific.frs.data.db;

import java.sql.Connection;
import java.sql.SQLException;

/** Factory Method for database connections, so tests and production can use different databases. */
@FunctionalInterface
public interface ConnectionFactory {
    Connection open() throws SQLException;
}
