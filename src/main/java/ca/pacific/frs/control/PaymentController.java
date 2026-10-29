package ca.pacific.frs.control;

import ca.pacific.frs.domain.exception.BusinessRuleException;
import ca.pacific.frs.domain.exception.NotFoundException;
import ca.pacific.frs.domain.exception.PaymentDeclinedException;
import ca.pacific.frs.domain.gateway.PaymentGateway;
import ca.pacific.frs.domain.gateway.PaymentGateway.GatewayResult;
import ca.pacific.frs.domain.model.CardDetails;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Payment;
import ca.pacific.frs.domain.model.PaymentStatus;
import ca.pacific.frs.domain.model.PaymentType;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.ReservationStatus;
import ca.pacific.frs.domain.repository.PaymentRepository;
import ca.pacific.frs.domain.repository.ReservationRepository;
import ca.pacific.frs.domain.repository.TransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Use case: Make a payment (simulated). Every attempt is recorded, including declines,
 * so the payment history shows exactly what happened.
 */
public final class PaymentController {
    private final TransactionManager tx;
    private final PaymentRepository payments;
    private final ReservationRepository reservations;
    private final PaymentGateway gateway;
    private final Session session;
    private final EventBus events;
    private final AccessGuard guard;
    private final Clock clock;

    public PaymentController(TransactionManager tx, PaymentRepository payments, ReservationRepository reservations,
                             PaymentGateway gateway, Session session, EventBus events, Clock clock) {
        this.tx = tx;
        this.payments = payments;
        this.reservations = reservations;
        this.gateway = gateway;
        this.session = session;
        this.events = events;
        this.guard = new AccessGuard(session);
        this.clock = clock;
    }

    /**
     * Pays the outstanding balance of a reservation that is awaiting payment and confirms it.
     *
     * @throws PaymentDeclinedException if the card is declined; the reservation stays on hold so the user can retry
     */
    public Payment payReservation(long reservationId, CardDetails card) {
        Reservation reservation = load(reservationId);
        guard.requireReservationAccess(reservation);
        if (reservation.status() != ReservationStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("This reservation is not awaiting payment.");
        }
        Money due = reservation.fare().total().minus(amountPaid(reservationId));
        Payment payment = due.isPositive() ? charge(reservationId, card, due) : null;
        if (payment != null && payment.status() != PaymentStatus.COMPLETED) {
            throw new PaymentDeclinedException(payment.failureReason());
        }
        tx.inTransaction(() -> {
            Reservation fresh = load(reservationId);
            fresh.confirm(now());
            reservations.update(fresh);
        });
        events.publish(EventBus.Topic.RESERVATIONS);
        return payment;
    }

    /**
     * Charges a card and records the attempt. Returns the payment in COMPLETED or DECLINED state.
     * Callers decide what a decline means for their use case.
     */
    Payment charge(long reservationId, CardDetails card, Money amount) {
        Payment payment = payments.insert(Payment.initiate(reservationId, PaymentType.CHARGE, amount, card, now()));
        GatewayResult auth = gateway.authorize(card, amount);
        if (!auth.approved()) {
            payment.decline(auth.declineReason());
        } else {
            payment.authorize(auth.reference());
            GatewayResult capture = gateway.capture(auth.reference(), amount);
            if (capture.approved()) {
                payment.complete();
            } else {
                payment.decline(capture.declineReason());
            }
        }
        payments.update(payment);
        events.publish(EventBus.Topic.PAYMENTS);
        return payment;
    }

    /** Refunds {@code amount} to the card of the most recent completed charge. */
    Payment refund(long reservationId, Money amount) {
        if (!amount.isPositive()) {
            return null;
        }
        List<Payment> history = payments.findByReservation(reservationId);
        Payment lastCharge = history.stream()
                .filter(p -> p.type() == PaymentType.CHARGE && p.status() == PaymentStatus.COMPLETED)
                .max(Comparator.comparing(Payment::createdAt).thenComparing(Payment::id))
                .orElseThrow(() -> new BusinessRuleException("There is no completed payment to refund."));

        Payment refund = payments.insert(Payment.initiateRefund(lastCharge, amount, now()));
        GatewayResult result = gateway.refund(lastCharge.transactionRef(), amount);
        if (!result.approved()) {
            refund.decline(result.declineReason());
            payments.update(refund);
            throw new BusinessRuleException("The refund could not be processed: " + result.declineReason());
        }
        refund.authorize(result.reference());
        refund.complete();
        payments.update(refund);

        // When everything paid has been returned, mark the original charges as refunded.
        if (!amountPaid(reservationId).isPositive()) {
            for (Payment p : history) {
                if (p.type() == PaymentType.CHARGE && p.status() == PaymentStatus.COMPLETED) {
                    p.markRefunded();
                    payments.update(p);
                }
            }
        }
        events.publish(EventBus.Topic.PAYMENTS);
        return refund;
    }

    /** Net amount currently held for a reservation: completed charges minus completed refunds. */
    public Money amountPaid(long reservationId) {
        return payments.findByReservation(reservationId).stream()
                .map(Payment::netEffect)
                .reduce(Money.ZERO, Money::plus);
    }

    public List<Payment> paymentsFor(long reservationId) {
        guard.requireReservationAccess(load(reservationId));
        return payments.findByReservation(reservationId);
    }

    private Reservation load(long reservationId) {
        return reservations.findById(reservationId).orElseThrow(() -> new NotFoundException("Reservation", reservationId));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
