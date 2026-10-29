package ca.pacific.frs.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A small rounded status chip. */
public class Badge extends JLabel {
    private Tone tone = Tone.NEUTRAL;

    public Badge() {
        this("", Tone.NEUTRAL);
    }

    public Badge(String text, Tone tone) {
        super(text);
        putClientProperty(FlatClientProperties.STYLE_CLASS, "small semibold");
        setBorder(BorderFactory.createEmptyBorder(UIScale.scale(2), UIScale.scale(9), UIScale.scale(2), UIScale.scale(9)));
        setTone(tone);
    }

    public final void setTone(Tone tone) {
        this.tone = tone;
        repaint();
    }

    public void set(String text, Tone tone) {
        setText(text);
        setTone(tone);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(tone.background());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        } finally {
            g2.dispose();
        }
        setForeground(tone.foreground());
        super.paintComponent(g);
    }
}
