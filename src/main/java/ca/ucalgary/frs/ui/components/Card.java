package ca.ucalgary.frs.ui.components;

import ca.ucalgary.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A rounded surface on layer 1 with a hairline border and a soft shadow, used to group related content.
 */
public class Card extends JPanel {
    private static final int ARC = 14;
    private static final int SHADOW = 3;

    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        int pad = UIScale.scale(16);
        int sh = UIScale.scale(SHADOW);
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad + sh, pad));
    }

    /** Sets the inner padding in unscaled pixels. */
    public Card padding(int top, int left, int bottom, int right) {
        int sh = UIScale.scale(SHADOW);
        setBorder(BorderFactory.createEmptyBorder(UIScale.scale(top), UIScale.scale(left),
                UIScale.scale(bottom) + sh, UIScale.scale(right)));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float arc = UIScale.scale((float) ARC);
            int sh = UIScale.scale(SHADOW);
            float w = getWidth() - 1;
            float h = getHeight() - 1 - sh;

            // soft shadow: a few translucent offset layers
            Color shadow = Palette.shadow();
            for (int i = sh; i >= 1; i--) {
                g2.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), 10));
                g2.fill(new RoundRectangle2D.Float(0, i, w, h, arc, arc));
            }
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
            g2.setColor(Palette.layer1());
            g2.fill(shape);
            g2.setColor(Palette.border());
            g2.setStroke(new BasicStroke(1f));
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
