package ca.ucalgary.frs.domain.exception;

/** The (simulated) payment gateway declined a charge. */
public class PaymentDeclinedException extends BusinessRuleException {
    public PaymentDeclinedException(String reason) {
        super("Payment declined: " + reason);
    }
}
