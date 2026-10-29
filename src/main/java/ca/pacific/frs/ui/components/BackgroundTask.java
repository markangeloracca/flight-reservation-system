package ca.pacific.frs.ui.components;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Template Method over {@link SwingWorker}: the skeleton (run off the EDT, then report on the EDT,
 * always run the finally-step) is fixed; callers supply only the work and the reactions.
 * Database and gateway calls must always go through this so the UI never freezes.
 *
 * <pre>
 * BackgroundTask.of(() -> controller.search(...))
 *     .onSuccess(table::setRows)
 *     .onFinally(() -> button.setEnabled(true))
 *     .start(this);
 * </pre>
 */
public class BackgroundTask<T> extends SwingWorker<T, Void> {
    private final Callable<T> work;
    private Consumer<T> onSuccess = r -> { };
    private Consumer<Throwable> onFailure;
    private Runnable onFinally = () -> { };
    private Component anchor;

    protected BackgroundTask(Callable<T> work) {
        this.work = work;
    }

    public static <T> BackgroundTask<T> of(Callable<T> work) {
        return new BackgroundTask<>(work);
    }

    public static BackgroundTask<Void> run(Runnable work) {
        return new BackgroundTask<>(() -> {
            work.run();
            return null;
        });
    }

    public BackgroundTask<T> onSuccess(Consumer<T> handler) {
        this.onSuccess = handler;
        return this;
    }

    /** Overrides the default failure handling (a toast on the anchor's window). */
    public BackgroundTask<T> onFailure(Consumer<Throwable> handler) {
        this.onFailure = handler;
        return this;
    }

    public BackgroundTask<T> onFinally(Runnable handler) {
        this.onFinally = handler;
        return this;
    }

    /** Starts the task. {@code anchor} locates the window for error toasts. */
    public void start(Component anchor) {
        this.anchor = anchor;
        execute();
    }

    @Override
    protected final T doInBackground() throws Exception {
        return work.call();
    }

    @Override
    protected final void done() {
        try {
            onSuccess.accept(get());
        } catch (ExecutionException e) {
            fail(e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (CancellationException ignored) {
            // cancelled by the caller; nothing to report
        } catch (RuntimeException e) {
            fail(e);
        } finally {
            onFinally.run();
        }
    }

    private void fail(Throwable error) {
        if (onFailure != null) {
            onFailure.accept(error);
        } else {
            UiErrors.show(anchor, error);
        }
    }
}
