package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.ValidationException;

import java.util.regex.Pattern;

/** Small helpers for domain invariants. Every failure is a user-readable {@link ValidationException}. */
public final class Validate {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()\\-.]{7,20}$");

    private Validate() {
    }

    public static String required(String field, String label, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, label + " is required.");
        }
        return value.trim();
    }

    public static String maxLength(String field, String label, String value, int max) {
        if (value != null && value.length() > max) {
            throw new ValidationException(field, label + " must be at most " + max + " characters.");
        }
        return value;
    }

    public static String email(String field, String value) {
        String v = required(field, "Email", value);
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException(field, "Enter a valid email address, such as name@example.com.");
        }
        return v.toLowerCase();
    }

    /** Optional phone: blank becomes {@code null}. */
    public static String optionalPhone(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException(field, "Enter a valid phone number.");
        }
        return v;
    }

    public static int range(String field, String label, int value, int min, int max) {
        if (value < min || value > max) {
            throw new ValidationException(field, label + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    public static <T> T notNull(String field, String label, T value) {
        if (value == null) {
            throw new ValidationException(field, label + " is required.");
        }
        return value;
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
