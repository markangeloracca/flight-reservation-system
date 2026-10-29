package ca.ucalgary.frs.ui.shell;

import ca.ucalgary.frs.ui.components.Icons;
import ca.ucalgary.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Sidebar navigation item with hover, selected, pressed and keyboard-focus states. */
class NavButton extends JToggleButton {
    NavButton(String text, String icon) {
        super(text);
        setIcon(Icons.tinted(icon, Icons.MEDIUM, () -> isSelected() ? Palette.accent() : Palette.muted()));
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(UIScale.scale(12));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setRequestFocusEnabled(false);
        setBorder(BorderFactory.createEmptyBorder(UIScale.scale(9), UIScale.scale(14), UIScale.scale(9), UIScale.scale(14)));
        getAccessibleContext().setAccessibleName(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = UIScale.scale(10);
            boolean pressed = getModel().isPressed();
            if (isSelected()) {
                g2.setColor(Palette.accentSoft());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(Palette.accent());
                int bar = UIScale.scale(3);
                g2.fillRoundRect(0, UIScale.scale(8), bar, getHeight() - UIScale.scale(16), bar, bar);
            } else if (pressed || getModel().isRollover()) {
                g2.setColor(pressed ? Palette.border() : Palette.layer2());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            }
            if (isFocusOwner()) {
                g2.setColor(Palette.accent());
                g2.setStroke(new BasicStroke(UIScale.scale(2f)));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
            }
        } finally {
            g2.dispose();
        }
        setForeground(isSelected() ? Palette.accent() : Palette.text());
        super.paintComponent(g);
    }
}
