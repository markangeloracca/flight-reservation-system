package ca.pacific.frs.domain.repository;

import java.util.function.Supplier;

/**
 * Runs a block of repository calls atomically. Nested calls join the outer transaction.
 * Exceptions thrown inside roll the whole block back and propagate unchanged.
 */
public interface TransactionManager {
    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
