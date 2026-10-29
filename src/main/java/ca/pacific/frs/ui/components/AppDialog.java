package ca.pacific.frs.ui.components;

import ca.pacific.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Base modal dialog: title and subtitle header, body, right-aligned button bar.
 * Esc and the close box call {@link #onCancel()}; the primary button is the default (Enter).
 */
public abstract class AppDialog extends JDialog {
    private final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 8", "push[][]"));
    private final javax.swing.JLabel subtitleLabel;
    private boolean busy;

    protected AppDialog(Component owner, String title, String subtitle) {
        super(owner == null ? null : SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!busy) {
                    onCancel();
                }
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new MigLayout("insets 20 24 4 24, wrap 1, gap 0 4", "[grow, fill]"));
        header.add(Typography.h2(title));
        subtitleLabel = Typography.caption(subtitle == null ? "" : subtitle);
        subtitleLabel.setVisible(subtitle != null);
        header.add(subtitleLabel, "hidemode 3");
        root.add(header, BorderLayout.NORTH);
        JPanel south = new JPanel(new MigLayout("insets 12 24 20 24, fillx", "[grow, fill]"));
        buttons.setOpaque(false);
        south.add(buttons);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "frs.cancel");
        getRootPane().getActionMap().put("frs.cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!busy) {
                    onCancel();
                }
            }
        });
    }

    /** Sets the body. Call from the subclass constructor, then {@link #present()}. */
    protected void setBody(JComponent body) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 24, 0, 24));
        wrap.add(body);
        getContentPane().add(wrap, BorderLayout.CENTER);
    }

    protected void setSubtitle(String text) {
        subtitleLabel.setText(text);
        subtitleLabel.setVisible(text != null);
    }

    protected JButton addSecondaryButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        buttons.add(b);
        return b;
    }

    protected JButton addPrimaryButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        buttons.add(b);
        getRootPane().setDefaultButton(b);
        return b;
    }

    /** While busy, Esc and the close box are ignored so a running operation is not abandoned. */
    protected void setBusy(boolean busy) {
        this.busy = busy;
        setCursor(busy ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR) : null);
    }

    protected boolean isBusy() {
        return busy;
    }

    protected void onCancel() {
        dispose();
    }

    /** Packs, centres over the owner and shows (blocks until closed). */
    protected void present() {
        // Two passes: the first gives wrapping labels their width, the second sizes them to fit their text.
        pack();
        validate();
        pack();
        setMinimumSize(getSize());
        Window owner = getOwner();
        setLocationRelativeTo(owner);
        setVisible(true);
    }
}
