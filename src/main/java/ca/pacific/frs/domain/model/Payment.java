package ca.pacific.frs.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A simulated payment record: a charge or a refund against a reservation.
 * Only the card brand and last four digits are kept; the full number and CVV are never stored.
 */
public final class Payment {
    private final Long id;
    private final long reservationId;
    private final PaymentType type;
    private final Money amount;
    private final PaymentMethodType method;
    private final String cardBrand;
    private final String cardLast4;
    private final String cardholderName;
    private PaymentStatus status;
    private String transactionRef;
    private String failureReason;
    private final LocalDateTime createdAt;

    public Payment(Long id, long reservationId, PaymentType type, Money amount, PaymentMethodType method,
                   String cardBrand, String cardLast4, String cardholderName, PaymentStatus status,
                   String transactionRef, String failureReason, LocalDateTime createdAt) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        this.id = id;
        this.reservationId = reservationId;
        this.type = Objects.requireNonNull(type);
        this.amount = amount;
        this.method = Objects.requireNonNull(method);
        this.cardBrand = cardBrand;
        this.cardLast4 = cardLast4;
        this.cardholderName = cardholderName;
        this.status = Objects.requireNonNull(status);
        this.transactionRef = transactionRef;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public static Payment initiate(long reservationId, PaymentType type, Money amount, CardDetails card, LocalDateTime now) {
        return new Payment(null, reservationId, type, amount, card.method(), card.brand(), card.last4(),
                card.cardholderName(), PaymentStatus.INITIATED, null, null, now);
    }

    /** A refund goes back to the card used for the original charge. */
    public static Payment initiateRefund(Payment originalCharge, Money amount, LocalDateTime now) {
        return new Payment(null, originalCharge.reservationId, PaymentType.REFUND, amount, originalCharge.method,
                originalCharge.cardBrand, originalCharge.cardLast4, originalCharge.cardholderName,
                PaymentStatus.INITIATED, null, null, now);
    }

    public Payment withId(long newId) {
        return new Payment(newId, reservationId, type, amount, method, cardBrand, cardLast4, cardholderName, status,
                transactionRef, failureReason, createdAt);
    }

    public void authorize(String ref) {
        moveTo(PaymentStatus.AUTHORIZED);
        this.transactionRef = ref;
    }

    public void complete() {
        moveTo(PaymentStatus.COMPLETED);
    }

    public void decline(String reason) {
        moveTo(PaymentStatus.DECLINED);
        this.failureReason = reason;
    }

    public void markRefunded() {
        moveTo(PaymentStatus.REFUNDED);
    }

    private void moveTo(PaymentStatus target) {
        if (!status.canMoveTo(target)) {
            throw new IllegalStateException("Payment cannot move from " + status + " to " + target);
        }
        this.status = target;
    }

    /** Signed effect on the amount paid: + for a completed charge, − for a completed refund, 0 otherwise. */
    public Money netEffect() {
        if (type == PaymentType.CHARGE && (status == PaymentStatus.COMPLETED || status == PaymentStatus.REFUNDED)) {
            return amount;
        }
        if (type == PaymentType.REFUND && status == PaymentStatus.COMPLETED) {
            return amount.negate();
        }
        return Money.ZERO;
    }

    public String maskedCard() {
        return cardBrand + " •••• " + cardLast4;
    }

    public Long id() {
        return id;
    }

    public long reservationId() {
        return reservationId;
    }

    public PaymentType type() {
        return type;
    }

    public Money amount() {
        return amount;
    }

    public PaymentMethodType method() {
        return method;
    }

    public String cardBrand() {
        return cardBrand;
    }

    public String cardLast4() {
        return cardLast4;
    }

    public String cardholderName() {
        return cardholderName;
    }

    public PaymentStatus status() {
        return status;
    }

    public String transactionRef() {
        return transactionRef;
    }

    public String failureReason() {
        return failureReason;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
