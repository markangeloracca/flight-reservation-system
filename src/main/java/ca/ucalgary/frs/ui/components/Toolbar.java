package ca.ucalgary.frs.ui.components;

import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Horizontal row of controls: filters on the left, actions pushed to the right. */
public class Toolbar extends JPanel {
    private boolean pushed;

    public Toolbar() {
        super(new MigLayout("insets 0, gap 8, fillx", "", "[center]"));
        setOpaque(false);
    }

    public Toolbar left(JComponent c, String constraints) {
        add(c, constraints);
        return this;
    }

    public Toolbar left(JComponent c) {
        return left(c, "");
    }

    public Toolbar right(JComponent c) {
        add(c, pushed ? "" : "pushx, alignx right");
        pushed = true;
        return this;
    }

    public static JButton button(String text, String icon, Runnable action) {
        JButton b = new JButton(text, icon == null ? null : Icons.get(icon));
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Accent-filled button for the main action of a screen. */
    public static final String PRIMARY_STYLE = "background: $Button.default.background;"
            + "foreground: $Button.default.foreground; borderColor: $Button.default.borderColor;"
            + "hoverBackground: $Button.default.hoverBackground; pressedBackground: $Button.default.pressedBackground;"
            + "focusedBorderColor: $Button.default.focusedBorderColor; font: bold";

    public static JButton primary(String text, String icon, Runnable action) {
        JButton b = button(text, null, action);
        b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, PRIMARY_STYLE);
        if (icon != null) {
            b.setIcon(Icons.tinted(icon, Icons.SMALL, b::getForeground));
        }
        return b;
    }
}
