package ca.ucalgary.frs.ui.components;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;

/**
 * Swaps between loading skeleton, content, and an empty state, so every list communicates what is
 * happening instead of showing a blank area.
 */
public class ContentPane extends JPanel {
    public enum State { LOADING, CONTENT, EMPTY }

    private final CardLayout cards = new CardLayout();
    private final EmptyState empty;
    private State state;

    public ContentPane(JComponent content, EmptyState empty) {
        this.empty = empty;
        setLayout(cards);
        setOpaque(false);
        add(new Skeleton(), State.LOADING.name());
        add(content, State.CONTENT.name());
        add(empty, State.EMPTY.name());
        show(State.CONTENT);
    }

    public void show(State s) {
        state = s;
        cards.show(this, s.name());
    }

    public void loading() {
        show(State.LOADING);
    }

    /** Shows the content, or the empty state when there are no items. */
    public void showResult(boolean hasItems) {
        show(hasItems ? State.CONTENT : State.EMPTY);
    }

    public EmptyState empty() {
        return empty;
    }

    public State state() {
        return state;
    }
}
