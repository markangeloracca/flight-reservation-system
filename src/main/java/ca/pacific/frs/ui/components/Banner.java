package ca.pacific.frs.ui.components;

import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Inline alert shown inside forms and dialogs. Hidden until {@link #show(Tone, String)} is called. */
public class Banner extends JPanel {
    private final JLabel icon = new JLabel();
    private final WrapLabel message = new WrapLabel("", 640);
    private Tone tone = Tone.NEUTRAL;

    public Banner() {
        super(new MigLayout("insets 8 10 8 10, gap 8", "[][grow, fill]", "[center]"));
        setOpaque(false);
        add(icon, "aligny top");
        add(message, "wmin 0");
        setVisible(false);
    }

    public void show(Tone tone, String text) {
        this.tone = tone;
        icon.setIcon(Icons.tinted(tone.icon(), Icons.SMALL, tone::foreground));
        message.setPlainText(text);
        message.setForeground(tone.foreground());
        getAccessibleContext().setAccessibleName(text);
        setVisible(true);
        revalidate();
        repaint();
    }

    public void hideBanner() {
        setVisible(false);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (message != null && tone != null) {
            message.setForeground(tone.foreground());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(tone.background());
            int arc = UIScale.scale(10);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
            g2.dispose();
        }
    }

    static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
