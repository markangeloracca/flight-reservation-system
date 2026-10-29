package ca.pacific.frs.ui.components;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * A label whose text wraps to the width it is given and reports the matching height, so layouts never
 * clip multi-line messages. Plain text only (it is escaped).
 */
public class WrapLabel extends JLabel {
    private final int maxPreferredWidth;
    private boolean center;

    public WrapLabel(String text, int maxPreferredWidth) {
        this.maxPreferredWidth = maxPreferredWidth;
        setPlainText(text);
    }

    public final void setPlainText(String text) {
        String body = Banner.escape(text == null ? "" : text);
        setText(center ? "<html><div style='text-align:center'>" + body + "</div></html>" : "<html>" + body + "</html>");
        getAccessibleContext().setAccessibleName(text);
    }

    public WrapLabel centered() {
        center = true;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        int cap = UIScale.scale(maxPreferredWidth);
        int width = getWidth() > 0 ? getWidth() : Math.min(natural.width, cap);
        return new Dimension(Math.min(width, cap), heightFor(width));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(UIScale.scale(80), getPreferredSize().height);
    }

    private int heightFor(int width) {
        View view = (View) getClientProperty(BasicHTML.propertyKey);
        Insets in = getInsets();
        if (view == null) {
            return super.getPreferredSize().height;
        }
        view.setSize(Math.max(1, width - in.left - in.right), 0);
        return (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + in.top + in.bottom;
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        int old = getWidth();
        super.setBounds(x, y, w, h);
        if (old != w && heightFor(w) != h) {
            SwingUtilities.invokeLater(this::revalidate);
        }
    }
}
