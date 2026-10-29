package ca.pacific.frs.ui.components;

import ca.pacific.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;

/** Shimmering placeholder rows shown while a list loads. The animation only runs while visible. */
public class Skeleton extends JComponent {
    private final Timer timer;
    private float phase;

    public Skeleton() {
        timer = new Timer(30, e -> {
            phase = (phase + 0.025f) % 1.6f;
            repaint();
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });
        getAccessibleContext().setAccessibleName("Loading");
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PROGRESS_BAR;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = UIScale.scale(16);
            int rowH = UIScale.scale(34);
            int barH = UIScale.scale(12);
            Color base = Palette.layer2();
            Color shine = Palette.hover();
            float x = (phase - 0.3f) * getWidth();
            g2.setPaint(new GradientPaint(x, 0, base, x + getWidth() * 0.3f, 0, shine, true));
            int[] widths = {22, 14, 18, 12, 16, 10};
            for (int y = pad, i = 0; y + rowH < getHeight(); y += rowH, i++) {
                int cx = pad;
                for (int c = 0; c < widths.length; c++) {
                    int w = (getWidth() - pad * 2) * (widths[(c + i) % widths.length]) / 100;
                    g2.fillRoundRect(cx, y + (rowH - barH) / 2, w - UIScale.scale(12), barH, barH, barH);
                    cx += w;
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
