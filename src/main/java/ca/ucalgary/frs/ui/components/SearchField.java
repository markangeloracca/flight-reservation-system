package ca.ucalgary.frs.ui.components;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.function.Consumer;

/** Text field with a search icon, clear button and debounced change notifications (250 ms). */
public class SearchField extends JTextField {
    private static final int DEBOUNCE_MS = 250;
    private final Timer debounce;

    public SearchField(String placeholder, Consumer<String> onChange) {
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.muted("search", Icons.SMALL));
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        getAccessibleContext().setAccessibleName(placeholder);
        debounce = new Timer(DEBOUNCE_MS, e -> onChange.accept(getText().trim()));
        debounce.setRepeats(false);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
            }
        });
    }
}
