package ca.ucalgary.frs.ui.shell;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.EventBus;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Common page plumbing. Subclasses implement {@link #reload()}; the page reloads when first shown and
 * whenever a subscribed kind of data changes. Changes that arrive while the page is hidden are applied
 * the next time it is shown, so hidden pages do no work.
 */
public abstract class BasePage extends JPanel implements Page {
    protected final AppContext ctx;
    private final List<Runnable> unsubscribers = new ArrayList<>();
    private boolean stale = true;

    protected BasePage(AppContext ctx) {
        super(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[]"));
        this.ctx = ctx;
        setOpaque(false);
    }

    /** Loads (or re-loads) the page's data. Must not block the EDT. */
    protected abstract void reload();

    protected final void reloadOn(EventBus.Topic... topics) {
        for (EventBus.Topic topic : topics) {
            PropertyChangeListener l = e -> SwingUtilities.invokeLater(() -> {
                if (isShowing()) {
                    reload();
                } else {
                    stale = true;
                }
            });
            ctx.events().subscribe(topic, l);
            unsubscribers.add(() -> ctx.events().unsubscribe(topic, l));
        }
    }

    @Override
    public void onShow() {
        if (stale) {
            stale = false;
            reload();
        }
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void dispose() {
        unsubscribers.forEach(Runnable::run);
        unsubscribers.clear();
    }
}
