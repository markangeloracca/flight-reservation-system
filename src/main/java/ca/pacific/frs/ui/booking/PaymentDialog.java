package ca.pacific.frs.ui.booking;

import ca.pacific.frs.domain.exception.ValidationException;
import ca.pacific.frs.ui.components.WrapLabel;
import ca.pacific.frs.domain.model.CardDetails;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.PaymentMethodType;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.FormBuilder;
import ca.pacific.frs.ui.components.Icons;
import ca.pacific.frs.ui.components.Segmented;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Component;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Optional;

/**
 * Collects card details and runs a payment through a caller-supplied action in the background.
 * Declines are shown inline so the user can correct the card and retry without losing their place.
 */
public final class PaymentDialog<T> extends AppDialog {

    /** The payment step to run with the entered card (for example, pay a hold or pay a fare difference). */
    @FunctionalInterface
    public interface Submitter<T> {
        T submit(CardDetails card) throws Exception;
    }

    private final Submitter<T> submitter;
    private final FormBuilder form = new FormBuilder(2);
    private final Segmented<PaymentMethodType> method = new Segmented<>(m -> { });
    private final JTextField name = new JTextField(22);
    private final JTextField number = new JTextField(22);
    private final JTextField expiry = new JTextField(6);
    private final JPasswordField cvv = new JPasswordField(4);
    private final JLabel brand = Typography.small("");
    private final JProgressBar progress = new JProgressBar();
    private final JButton pay;
    private final JButton cancel;
    private T result;

    private PaymentDialog(Component owner, String title, String summary, Money amount, String cardholder,
                          Submitter<T> submitter) {
        super(owner, title, summary);
        this.submitter = submitter;

        method.add(PaymentMethodType.CREDIT_CARD, "Credit card").add(PaymentMethodType.DEBIT_CARD, "Debit card");
        name.setText(cardholder);
        number.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "1234 5678 9012 3456");
        number.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.muted("credit-card", Icons.SMALL));
        number.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, brand);
        ((AbstractDocument) number.getDocument()).setDocumentFilter(new GroupingFilter(19, 4, ' '));
        number.getDocument().addDocumentListener(SimpleDocumentListener.of(this::updateBrand));
        expiry.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "MM/YY");
        ((AbstractDocument) expiry.getDocument()).setDocumentFilter(new GroupingFilter(4, 2, '/'));
        cvv.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "123");
        ((AbstractDocument) cvv.getDocument()).setDocumentFilter(new GroupingFilter(4, 99, ' '));

        JPanel amountRow = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        amountRow.setOpaque(false);
        amountRow.add(Typography.caption("Amount due"));
        amountRow.add(Typography.h2(amount.toString()));

        form.row(amountRow)
                .row(method)
                .field("cardholderName", "Name on card", name, 2)
                .field("number", "Card number", number, 2)
                .field("expiry", "Expiry", expiry)
                .field("cvv", "CVV", cvv);
        WrapLabel hint = new WrapLabel(
                "Payments are simulated. Try 4242 4242 4242 4242 with any future expiry. "
                        + "Cards ending in 0002 are declined.", 420);
        Typography.styled(hint, "small");
        Typography.mutedText(hint);
        form.row(hint);
        progress.setIndeterminate(true);
        progress.setVisible(false);
        form.row(progress);

        JPanel body = form.panel();
        body.setPreferredSize(new java.awt.Dimension(com.formdev.flatlaf.util.UIScale.scale(440),
                body.getPreferredSize().height));
        setBody(body);
        cancel = addSecondaryButton("Cancel", this::onCancel);
        pay = addPrimaryButton("Pay " + amount, this::submit);
    }

    /**
     * Shows the dialog and blocks until it closes.
     *
     * @return the submitter's result, or empty if the user cancelled
     */
    public static <T> Optional<T> show(Component owner, String title, String summary, Money amount,
                                       String cardholder, Submitter<T> submitter) {
        PaymentDialog<T> d = new PaymentDialog<>(owner, title, summary, amount, cardholder, submitter);
        d.present();
        return Optional.ofNullable(d.result);
    }

    private void submit() {
        form.clearErrors();
        CardDetails card;
        char[] cvvChars = cvv.getPassword();
        try {
            card = new CardDetails(method.selected(), name.getText(), number.getText(), parseExpiry(expiry.getText()),
                    new String(cvvChars));
        } catch (ValidationException e) {
            form.showError(e);
            return;
        } finally {
            Arrays.fill(cvvChars, '\0');
        }
        setWorking(true);
        BackgroundTask.of(() -> submitter.submit(card))
                .onSuccess(r -> {
                    result = r;
                    dispose();
                })
                .onFailure(form::showError)
                .onFinally(() -> setWorking(false))
                .start(this);
    }

    private void setWorking(boolean working) {
        setBusy(working);
        progress.setVisible(working);
        for (JComponent c : new JComponent[]{name, number, expiry, cvv, method, pay, cancel}) {
            c.setEnabled(!working);
        }
        pay.setText(working ? "Processing…" : pay.getText().startsWith("Processing") ? "Try again" : pay.getText());
        pack();
    }

    static YearMonth parseExpiry(String text) {
        String digits = text.replaceAll("\\D", "");
        if (digits.length() != 4) {
            throw new ValidationException("expiry", "Enter the expiry as MM/YY.");
        }
        int month = Integer.parseInt(digits.substring(0, 2));
        if (month < 1 || month > 12) {
            throw new ValidationException("expiry", "Month must be between 01 and 12.");
        }
        return YearMonth.of(2000 + Integer.parseInt(digits.substring(2)), month);
    }

    private void updateBrand() {
        String d = number.getText().replaceAll("\\D", "");
        String b = d.startsWith("4") ? "Visa" : d.matches("^(5[1-5]|2[2-7]).*") ? "Mastercard"
                : d.matches("^3[47].*") ? "Amex" : "";
        brand.setText(b.isEmpty() ? "" : b + "  ");
    }

    /** Keeps only digits, caps the length, and inserts a separator every {@code group} digits. */
    static final class GroupingFilter extends DocumentFilter {
        private final int maxDigits;
        private final int group;
        private final char separator;

        GroupingFilter(int maxDigits, int group, char separator) {
            this.maxDigits = maxDigits;
            this.group = group;
            this.separator = separator;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            replace(fb, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
            String digits = next.replaceAll("\\D", "");
            if (digits.length() > maxDigits) {
                digits = digits.substring(0, maxDigits);
            }
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % group == 0) {
                    out.append(separator);
                }
                out.append(digits.charAt(i));
            }
            fb.replace(0, fb.getDocument().getLength(), out.toString(), attrs);
        }
    }

    /** Lambda-friendly document listener. */
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void changed();

        static SimpleDocumentListener of(Runnable r) {
            return r::run;
        }

        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent e) {
            changed();
        }

        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent e) {
            changed();
        }

        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent e) {
            changed();
        }
    }

}
