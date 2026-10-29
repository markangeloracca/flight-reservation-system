package ca.ucalgary.frs.ui.components;

import ca.ucalgary.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-blocking notifications that fade in at the bottom-right of a window and dismiss themselves.
 * Click a toast to close it early.
 */
public final class Toasts {
    private static final int FADE_MS = 180;
    private static final String KEY_STACK = "frs.toastStack";

    private Toasts() {
    }

    public static void success(Component anchor, String message) {
        show(anchor, Tone.SUCCESS, message, 3500);
    }

    public static void info(Component anchor, String message) {
        show(anchor, Tone.ACCENT, message, 3500);
    }

    public static void warning(Component anchor, String message) {
        show(anchor, Tone.WARNING, message, 5000);
    }

    public static void error(Component anchor, String message) {
        show(anchor, Tone.DANGER, message, 6500);
    }

    public static void show(Component anchor, Tone tone, String message, int millis) {
        JRootPane root = anchor == null ? null : SwingUtilities.getRootPane(anchor);
        if (root == null) {
            return;
        }
        JLayeredPane layers = root.getLayeredPane();
        Stack stack = stackFor(layers);
        Toast toast = new Toast(tone, message, stack);
        stack.add(toast);
        toast.fadeIn(millis);
    }

    private static Stack stackFor(JLayeredPane layers) {
        Object existing = layers.getClientProperty(KEY_STACK);
        if (existing instanceof Stack s) {
            return s;
        }
        Stack s = new Stack(layers);
        layers.putClientProperty(KEY_STACK, s);
        return s;
    }

    /** Keeps toasts stacked upwards from the bottom-right corner, re-laid out on resize. */
    private static final class Stack {
        private final JLayeredPane layers;
        private final List<Toast> toasts = new ArrayList<>();

        Stack(JLayeredPane layers) {
            this.layers = layers;
            layers.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    layout();
                }
            });
        }

        void add(Toast t) {
            toasts.add(t);
            layers.add(t, JLayeredPane.POPUP_LAYER);
            layout();
        }

        void remove(Toast t) {
            toasts.remove(t);
            layers.remove(t);
            layers.repaint();
            layout();
        }

        void layout() {
            int margin = UIScale.scale(20);
            int gap = UIScale.scale(8);
            int y = layers.getHeight() - margin;
            for (int i = toasts.size() - 1; i >= 0; i--) {
                Toast t = toasts.get(i);
                Dimension d = t.getPreferredSize();
                int w = Math.min(d.width, UIScale.scale(420));
                t.setSize(w, t.getPreferredHeight(w));
                y -= t.getHeight();
                t.setLocation(layers.getWidth() - w - margin, y);
                y -= gap;
            }
        }
    }

    private static final class Toast extends JPanel {
        private final Tone tone;
        private final Stack stack;
        private final JLabel text;
        private float alpha = 0f;
        private Timer animation;

        Toast(Tone tone, String message, Stack stack) {
            super(new MigLayout("insets 10 12 10 14, gap 10", "[][grow, fill]", "[center]"));
            this.tone = tone;
            this.stack = stack;
            setOpaque(false);
            add(new JLabel(Icons.tinted(tone.icon(), Icons.MEDIUM, tone::foreground)), "aligny top");
            text = new JLabel("<html>" + Banner.escape(message) + "</html>");
            text.setForeground(Palette.text());
            add(text, "wmin 0");
            getAccessibleContext().setAccessibleName(message);
            setToolTipText("Click to dismiss");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    fadeOut();
                }
            });
        }

        int getPreferredHeight(int width) {
            text.setSize(width - UIScale.scale(56), Short.MAX_VALUE);
            setSize(width, Short.MAX_VALUE);
            return Math.max(getPreferredSize().height, UIScale.scale(44));
        }

        void fadeIn(int visibleMillis) {
            animate(0f, 1f, () -> {
                Timer hold = new Timer(visibleMillis, e -> fadeOut());
                hold.setRepeats(false);
                hold.start();
            });
        }

        void fadeOut() {
            if (getParent() == null) {
                return;
            }
            animate(alpha, 0f, () -> stack.remove(this));
        }

        private void animate(float from, float to, Runnable then) {
            if (animation != null) {
                animation.stop();
            }
            long start = System.currentTimeMillis();
            animation = new Timer(15, null);
            animation.addActionListener(e -> {
                float t = Math.min(1f, (System.currentTimeMillis() - start) / (float) FADE_MS);
                float eased = t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
                alpha = from + (to - from) * eased;
                repaint();
                if (t >= 1f) {
                    ((Timer) e.getSource()).stop();
                    then.run();
                }
            });
            animation.start();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = UIScale.scale(12);
                g2.setColor(Palette.layer1());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.setColor(tone.foreground());
                g2.fillRoundRect(0, 0, UIScale.scale(4), getHeight() - 1, UIScale.scale(4), UIScale.scale(4));
                g2.setColor(Palette.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            } finally {
                g2.dispose();
            }
        }
    }

    /** For components that are not yet in a window: shows nothing. */
    static boolean canShow(JComponent c) {
        return SwingUtilities.getRootPane(c) != null;
    }
}
