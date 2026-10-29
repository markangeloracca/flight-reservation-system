package ca.ucalgary.frs.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Segmented control: a row of mutually exclusive toggle buttons. */
public class Segmented<T> extends JPanel {
    private final ButtonGroup group = new ButtonGroup();
    private final List<T> values = new ArrayList<>();
    private final List<JToggleButton> buttons = new ArrayList<>();
    private T selected;

    public Segmented(Consumer<T> onChange) {
        super(new MigLayout("insets 0, gap 4"));
        setOpaque(false);
        this.onChange = onChange;
    }

    private final Consumer<T> onChange;

    public Segmented<T> add(T value, String label) {
        JToggleButton b = new JToggleButton(label);
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TAB);
        b.putClientProperty(FlatClientProperties.STYLE, "tab.selectedBackground: $Frs.accentSoftColor;"
                + "tab.selectedForeground: $Frs.accentColor; tab.underlineHeight: 0; arc: 999; margin: 4,12,4,12");
        b.addActionListener(e -> {
            if (!value.equals(selected)) {
                selected = value;
                onChange.accept(value);
            }
        });
        group.add(b);
        values.add(value);
        buttons.add(b);
        add(b);
        if (selected == null) {
            select(value);
        }
        return this;
    }

    public void select(T value) {
        int i = values.indexOf(value);
        if (i >= 0) {
            buttons.get(i).setSelected(true);
            selected = value;
        }
    }

    public T selected() {
        return selected;
    }
}
