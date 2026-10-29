package ca.ucalgary.frs.data.db;

import ca.ucalgary.frs.domain.exception.DuplicateKeyException;
import ca.ucalgary.frs.domain.exception.PersistenceException;
import ca.ucalgary.frs.domain.repository.TransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * Unit-of-work coordinator. {@link #inTransaction} binds one connection to the current thread so
 * every repository call inside the block joins the same transaction. Outside a transaction,
 * {@link #withConnection} opens a short-lived auto-commit connection.
 */
public final class Database implements TransactionManager {
    private final ConnectionFactory factory;
    private final ThreadLocal<Connection> bound = new ThreadLocal<>();

    public Database(ConnectionFactory factory) {
        this.factory = factory;
    }

    /** Runs repository work, joining the current transaction if there is one. */
    public <T> T withConnection(SqlWork<T> work) {
        Connection current = bound.get();
        try {
            if (current != null) {
                return work.run(current);
            }
            try (Connection c = factory.open()) {
                return work.run(c);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Maps SQLite errors to domain exceptions. Primary code 19 is SQLITE_CONSTRAINT (extended codes keep it in the low byte). */
    static PersistenceException translate(SQLException e) {
        String message = String.valueOf(e.getMessage());
        if ((e.getErrorCode() & 0xFF) == 19 && message.contains("UNIQUE")) {
            return new DuplicateKeyException(message, e);
        }
        return new PersistenceException("Database operation failed: " + message, e);
    }

    /**
     * Runs {@code work} atomically. Nested calls join the outer transaction. Any exception rolls back
     * and is rethrown unchanged (so business exceptions reach the caller as-is).
     */
    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (bound.get() != null) {
            return work.get();
        }
        Connection c;
        try {
            c = factory.open();
            c.setAutoCommit(false);
        } catch (SQLException e) {
            throw new PersistenceException("Could not open a database connection", e);
        }
        bound.set(c);
        try {
            T result = work.get();
            c.commit();
            return result;
        } catch (RuntimeException | Error e) {
            rollbackQuietly(c, e);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(c, e);
            throw translate(e);
        } finally {
            bound.remove();
            try {
                c.setAutoCommit(true);
                c.close();
            } catch (SQLException ignored) {
                // connection is being discarded
            }
        }
    }

    private static void rollbackQuietly(Connection c, Throwable cause) {
        try {
            c.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
        }
    }

    /** Called on shutdown: refresh query-planner stats and fold the WAL back into the main file. */
    public void optimizeAndCheckpoint() {
        withConnection(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("PRAGMA optimize");
                s.execute("PRAGMA wal_checkpoint(TRUNCATE)");
            }
            return null;
        });
    }
}
