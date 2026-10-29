package ca.ucalgary.frs.ui.components;

import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds vertical forms (label above field) with per-field error messages. Fields are registered under
 * the same keys that domain {@link ValidationException}s use, so errors land beside the right input.
 */
public class FormBuilder {
    private final JPanel panel;
    private final Banner banner = new Banner();
    private final Map<String, Field> fields = new LinkedHashMap<>();

    private record Field(JComponent input, JLabel error) {
    }

    public FormBuilder(int columns) {
        StringBuilder cols = new StringBuilder();
        for (int i = 0; i < columns; i++) {
            cols.append("[grow, fill, sg col]");
        }
        panel = new JPanel(new MigLayout("insets 0, fillx, wrap " + columns + ", gapx 12, gapy 0", cols.toString()));
        panel.setOpaque(false);
        panel.add(banner, "span, growx, gapbottom 12, hidemode 3");
    }

    /** Adds a labelled field occupying one column. */
    public FormBuilder field(String key, String label, JComponent input) {
        return field(key, label, input, 1);
    }

    public FormBuilder field(String key, String label, JComponent input, int span) {
        JPanel cell = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 0 4", "[grow, fill]"));
        cell.setOpaque(false);
        JLabel l = Typography.strong(label);
        l.setLabelFor(input);
        if (input.getAccessibleContext().getAccessibleName() == null) {
            input.getAccessibleContext().setAccessibleName(label);
        }
        JLabel error = Typography.small("");
        error.putClientProperty(FlatClientProperties.STYLE, "foreground: $Frs.dangerColor");
        error.setVisible(false);
        cell.add(l);
        cell.add(input);
        cell.add(error, "hidemode 3");
        panel.add(cell, "span " + span + ", gapbottom 12");
        fields.put(key, new Field(input, error));
        return this;
    }

    /** Adds an arbitrary full-width component (a hint, a separator, a heading). */
    public FormBuilder row(JComponent c) {
        panel.add(c, "span, growx, gapbottom 12");
        return this;
    }

    public JPanel panel() {
        return panel;
    }

    public Banner banner() {
        return banner;
    }

    /** Shows a validation error under its field (or in the banner if the field is unknown) and focuses it. */
    public void showError(Throwable error) {
        clearErrors();
        if (error instanceof ValidationException ve && ve.field() != null && fields.containsKey(ve.field())) {
            Field f = fields.get(ve.field());
            f.error().setText(ve.getMessage());
            f.error().setVisible(true);
            f.input().putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            f.input().requestFocusInWindow();
        } else {
            banner.show(error instanceof ca.ucalgary.frs.domain.exception.BusinessRuleException ? Tone.WARNING : Tone.DANGER,
                    UiErrors.message(error));
        }
        panel.revalidate();
        repackDialog();
    }

    private void repackDialog() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (javax.swing.SwingUtilities.getWindowAncestor(panel) instanceof javax.swing.JDialog d) {
                d.pack();
            }
        });
    }

    public void clearErrors() {
        banner.hideBanner();
        for (Field f : fields.values()) {
            f.error().setVisible(false);
            f.input().putClientProperty(FlatClientProperties.OUTLINE, null);
        }
    }
}
