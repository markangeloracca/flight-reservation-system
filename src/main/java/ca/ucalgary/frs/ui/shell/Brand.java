package ca.ucalgary.frs.ui.shell;

import ca.ucalgary.frs.ui.components.Icons;
import ca.ucalgary.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Supplier;

/** The application mark: a plane in a filled circle. */
public class Brand extends JComponent {
    private final int size;
    private final Supplier<Color> fill;
    private final Supplier<Color> glyph;

    public Brand(int size) {
        this(size, Palette::accent, Palette::onAccent);
    }

    public Brand(int size, Supplier<Color> fill, Supplier<Color> glyph) {
        this.size = size;
        this.fill = fill;
        this.glyph = glyph;
        getAccessibleContext().setAccessibleName("Flight Reservation System");
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.ICON;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(UIScale.scale(size), UIScale.scale(size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s = Math.min(getWidth(), getHeight());
            g2.setColor(fill.get());
            g2.fillRoundRect(0, 0, s, s, s / 2, s / 2);
            var icon = Icons.tinted("plane", Math.round(size * 0.56f), glyph);
            icon.paintIcon(this, g2, (s - icon.getIconWidth()) / 2, (s - icon.getIconHeight()) / 2);
        } finally {
            g2.dispose();
        }
    }
}
