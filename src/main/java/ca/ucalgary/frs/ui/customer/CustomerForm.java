package ca.ucalgary.frs.ui.customer;

import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.ui.components.FormBuilder;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Reusable customer profile fields, used by registration, the profile page and the agent's editor. */
public class CustomerForm {
    private final FormBuilder form = new FormBuilder(2);
    private final JTextField first = new JTextField(14);
    private final JTextField last = new JTextField(14);
    private final JTextField email = new JTextField(24);
    private final JTextField phone = new JTextField(14);
    private final JTextField dob = new JTextField(10);
    private final JTextField address = new JTextField(24);
    private Customer original;

    public CustomerForm() {
        email.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "name@example.com");
        phone.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Optional");
        dob.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD (optional)");
        address.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Optional");
        form.field("firstName", "First name", first)
                .field("lastName", "Last name", last)
                .field("email", "Email", email, 2)
                .field("phone", "Phone", phone)
                .field("dateOfBirth", "Date of birth", dob)
                .field("address", "Address", address, 2);
    }

    public FormBuilder form() {
        return form;
    }

    public JPanel panel() {
        return form.panel();
    }

    public JTextField firstField() {
        return first;
    }

    public void setCustomer(Customer c) {
        original = c;
        first.setText(c == null ? "" : c.firstName());
        last.setText(c == null ? "" : c.lastName());
        email.setText(c == null ? "" : c.email());
        phone.setText(c == null || c.phone() == null ? "" : c.phone());
        dob.setText(c == null || c.dateOfBirth() == null ? "" : c.dateOfBirth().toString());
        address.setText(c == null || c.address() == null ? "" : c.address());
        form.clearErrors();
    }

    /** Builds a validated customer from the fields, keeping the id of the customer being edited. */
    public Customer toCustomer() {
        Customer.Builder b = original == null ? Customer.builder() : original.toBuilder();
        return b.firstName(first.getText())
                .lastName(last.getText())
                .email(email.getText())
                .phone(phone.getText())
                .dateOfBirth(parseDate(dob.getText()))
                .address(address.getText())
                .build();
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("dateOfBirth", "Use the format YYYY-MM-DD, for example 1990-04-25.");
        }
    }
}
