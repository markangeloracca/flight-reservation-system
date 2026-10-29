package ca.pacific.frs.domain.model;

import ca.pacific.frs.domain.exception.ValidationException;

import java.time.YearMonth;

/**
 * Card data entered at checkout. Validated on construction (Luhn checksum, expiry, CVV).
 * Lives only in memory for the duration of a payment and is never persisted.
 */
public record CardDetails(PaymentMethodType method, String cardholderName, String number, YearMonth expiry, String cvv) {

    public CardDetails {
        Validate.notNull("method", "Payment method", method);
        cardholderName = Validate.required("cardholderName", "Name on card", cardholderName);
        number = number == null ? "" : number.replaceAll("[\\s-]", "");
        if (!number.matches("\\d{13,19}") || !luhnValid(number)) {
            throw new ValidationException("number", "Enter a valid card number.");
        }
        Validate.notNull("expiry", "Expiry date", expiry);
        if (expiry.isBefore(YearMonth.now())) {
            throw new ValidationException("expiry", "This card has expired.");
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new ValidationException("cvv", "CVV must be 3 or 4 digits.");
        }
    }

    public String last4() {
        return number.substring(number.length() - 4);
    }

    public String brand() {
        if (method == PaymentMethodType.DEBIT_CARD) {
            return "Debit";
        }
        if (number.startsWith("4")) {
            return "Visa";
        }
        if (number.matches("^(5[1-5]|2[2-7]).*")) {
            return "Mastercard";
        }
        if (number.matches("^3[47].*")) {
            return "Amex";
        }
        return "Card";
    }

    public static boolean luhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** Never print the card number or CVV. */
    @Override
    public String toString() {
        return "CardDetails[" + brand() + " ****" + last4() + "]";
    }
}
