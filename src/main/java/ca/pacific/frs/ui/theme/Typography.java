package ca.pacific.frs.ui.theme;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * The type scale, mapped onto FlatLaf style classes so sizes scale with the base font and DPI.
 * Display › H1 › H2 › H3 › Body › Caption; Mono for codes and amounts.
 */
public final class Typography {
    private Typography() {
    }

    public static JLabel display(String text) {
        return styled(new JLabel(text), "h00");
    }

    public static JLabel h1(String text) {
        return styled(new JLabel(text), "h1");
    }

    public static JLabel h2(String text) {
        return styled(new JLabel(text), "h2");
    }

    public static JLabel h3(String text) {
        return styled(new JLabel(text), "h3");
    }

    public static JLabel body(String text) {
        return new JLabel(text);
    }

    public static JLabel strong(String text) {
        return styled(new JLabel(text), "semibold");
    }

    /** Secondary text in the muted colour. */
    public static JLabel caption(String text) {
        JLabel l = new JLabel(text);
        mutedText(l);
        return l;
    }

    public static JLabel small(String text) {
        JLabel l = styled(new JLabel(text), "small");
        mutedText(l);
        return l;
    }

    public static JLabel mono(String text) {
        return styled(new JLabel(text), "monospaced");
    }

    public static <T extends JComponent> T styled(T c, String styleClass) {
        c.putClientProperty(FlatClientProperties.STYLE_CLASS, styleClass);
        return c;
    }

    /** Muted foreground that follows theme switches. */
    public static void mutedText(JComponent c) {
        c.putClientProperty(FlatClientProperties.STYLE, "foreground: $Frs.mutedColor");
    }
}
