package ca.ucalgary.frs.ui.shell;

import javax.swing.JComponent;

/** A screen in the main window's navigation. */
public interface Page {
    String title();

    String subtitle();

    /** Icon name from {@code /icons}. */
    String icon();

    JComponent view();

    /** Called each time the page becomes visible. */
    default void onShow() {
    }

    /** Moves focus to the page's search field (Ctrl/Cmd+F). */
    default void focusSearch() {
    }

    /** Releases listeners when the user signs out. */
    default void dispose() {
    }
}
