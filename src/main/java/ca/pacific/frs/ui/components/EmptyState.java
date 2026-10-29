package ca.pacific.frs.ui.components;

import ca.pacific.frs.ui.theme.Palette;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Icon + title + explanation + optional call to action, shown instead of an empty table. */
public class EmptyState extends JPanel {
    private final JLabel title;
    private final WrapLabel message;
    private final JButton action = new JButton();
    private final IconBubble bubble;

    public EmptyState(String iconName, String titleText, String messageText) {
        super(new MigLayout("insets 32, wrap 1, alignx center, aligny center", "[center]", "[]16[]6[]16[]"));
        setOpaque(false);
        bubble = new IconBubble(iconName);
        title = Typography.h3(titleText);
        message = new WrapLabel("", 420).centered();
        Typography.mutedText(message);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        setMessage(messageText);
        action.setVisible(false);
        add(bubble);
        add(title);
        add(message, "w 420!");
        add(action);
    }

    public EmptyState set(String iconName, String titleText, String messageText) {
        bubble.iconName = iconName;
        bubble.repaint();
        title.setText(titleText);
        setMessage(messageText);
        return this;
    }

    private void setMessage(String text) {
        message.setPlainText(text);
    }

    /** Shows a call-to-action button. Pass {@code null} to hide it. */
    public EmptyState action(String label, Runnable onClick) {
        for (var l : action.getActionListeners()) {
            action.removeActionListener(l);
        }
        action.setVisible(label != null);
        if (label != null) {
            action.setText(label);
            action.addActionListener(e -> onClick.run());
        }
        return this;
    }

    private static final class IconBubble extends JLabel {
        private String iconName;

        IconBubble(String iconName) {
            this.iconName = iconName;
        }

        @Override
        public java.awt.Dimension getPreferredSize() {
            int s = UIScale.scale(64);
            return new java.awt.Dimension(s, s);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Palette.accentSoft());
                g2.fillOval(0, 0, getWidth(), getHeight());
                var icon = Icons.accent(iconName, 28);
                icon.paintIcon(this, g2, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);
            } finally {
                g2.dispose();
            }
        }
    }
}
