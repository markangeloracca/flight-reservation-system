package ca.ucalgary.frs.control;

import ca.ucalgary.frs.control.confirmation.ConfirmationRenderer;
import ca.ucalgary.frs.domain.exception.BusinessRuleException;
import ca.ucalgary.frs.domain.exception.DuplicateKeyException;
import ca.ucalgary.frs.domain.exception.NotFoundException;
import ca.ucalgary.frs.domain.exception.PaymentDeclinedException;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.gateway.FileExporter;
import ca.ucalgary.frs.domain.model.BookingConfirmation;
import ca.ucalgary.frs.domain.model.CardDetails;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.domain.model.FareQuote;
import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.domain.model.Payment;
import ca.ucalgary.frs.domain.model.PaymentStatus;
import ca.ucalgary.frs.domain.model.Permission;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.ReservationStatus;
import ca.ucalgary.frs.domain.model.Seat;
import ca.ucalgary.frs.domain.model.SeatClass;
import ca.ucalgary.frs.domain.model.User;
import ca.ucalgary.frs.domain.policy.ConfirmationCodeGenerator;
import ca.ucalgary.frs.domain.policy.FareCalculator;
import ca.ucalgary.frs.domain.policy.RefundPolicy;
import ca.ucalgary.frs.domain.repository.CustomerRepository;
import ca.ucalgary.frs.domain.repository.FlightQueryRepository;
import ca.ucalgary.frs.domain.repository.PaymentRepository;
import ca.ucalgary.frs.domain.repository.ReservationRepository;
import ca.ucalgary.frs.domain.repository.TransactionManager;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Facade over the booking use cases: Book a flight (hold seat, then pay), Cancel a reservation,
 * Modify a reservation, View reservations, Generate a booking confirmation.
 */
public final class BookingController {
    /** An unpaid seat hold is released after this long. */
    public static final Duration HOLD_EXPIRY = Duration.ofMinutes(15);
    private static final int CODE_ATTEMPTS = 20;

    private final TransactionManager tx;
    private final ReservationRepository reservations;
    private final FlightQueryRepository flights;
    private final CustomerRepository customers;
    private final PaymentRepository paymentRecords;
    private final PaymentController payments;
    private final PromotionController promotions;
    private final FareCalculator fareCalculator;
    private final RefundPolicy refundPolicy;
    private final ConfirmationCodeGenerator codes;
    private final FileExporter exporter;
    private final Session session;
    private final EventBus events;
    private final AccessGuard guard;
    private final Clock clock;

    public BookingController(TransactionManager tx, ReservationRepository reservations, FlightQueryRepository flights,
                             CustomerRepository customers, PaymentRepository paymentRecords, PaymentController payments,
                             PromotionController promotions, FareCalculator fareCalculator, RefundPolicy refundPolicy,
                             ConfirmationCodeGenerator codes, FileExporter exporter, Session session, EventBus events,
                             Clock clock) {
        this.tx = tx;
        this.reservations = reservations;
        this.flights = flights;
        this.customers = customers;
        this.paymentRecords = paymentRecords;
        this.payments = payments;
        this.promotions = promotions;
        this.fareCalculator = fareCalculator;
        this.refundPolicy = refundPolicy;
        this.codes = codes;
        this.exporter = exporter;
        this.session = session;
        this.events = events;
        this.guard = new AccessGuard(session);
        this.clock = clock;
    }

    // ------------------------------------------------------------------ pricing

    public FareQuote quote(Flight flight, SeatClass seatClass) {
        return fareCalculator.quote(flight, seatClass, promotions.currentDiscountPolicy());
    }

    // ------------------------------------------------------------------ book

    /**
     * Step 1 of booking: reserves the seat for the customer in PENDING_PAYMENT state.
     * The database's unique index guarantees two holds can never share a seat.
     */
    public Reservation holdSeat(long customerId, long flightId, String seatCode) {
        User user = session.require(Permission.BOOK_FLIGHTS);
        guard.requireCustomerAccess(customerId);
        try {
            Reservation held = tx.inTransaction(() -> {
                Customer customer = customers.findById(customerId)
                        .orElseThrow(() -> new NotFoundException("Customer", customerId));
                Flight flight = flights.findById(flightId).orElseThrow(() -> new NotFoundException("Flight", flightId));
                Seat seat = flight.aircraft().requireSeat(seatCode);
                if (flights.occupiedSeats(flightId).contains(seat.code())) {
                    throw seatTaken(seat);
                }
                Reservation r = Reservation.hold(uniqueCode(), customer, flight, seat, quote(flight, seat.seatClass()),
                        now(), user.id());
                return reservations.insert(r);
            });
            events.publish(EventBus.Topic.RESERVATIONS);
            events.publish(EventBus.Topic.FLIGHTS);
            return held;
        } catch (DuplicateKeyException e) {
            throw new BusinessRuleException("Seat " + seatCode + " was just taken. Please choose another seat.", e);
        }
    }

    /** Step 2 of booking: pays and confirms the hold. Declines leave the hold in place for a retry. */
    public Reservation pay(long reservationId, CardDetails card) {
        payments.payReservation(reservationId, card);
        return get(reservationId);
    }

    /** Abandons an unpaid hold and frees the seat. Does nothing if the hold is already gone. */
    public void releaseHold(long reservationId) {
        Reservation r = get(reservationId);
        if (r.status() != ReservationStatus.PENDING_PAYMENT) {
            return;
        }
        tx.inTransaction(() -> {
            Reservation fresh = get(reservationId);
            fresh.cancel(now());
            reservations.update(fresh);
            payments.refund(reservationId, payments.amountPaid(reservationId));
        });
        events.publish(EventBus.Topic.RESERVATIONS);
        events.publish(EventBus.Topic.FLIGHTS);
    }

    // ------------------------------------------------------------------ cancel

    public record CancellationQuote(Reservation reservation, Money amountPaid, Money refund, String policy) {
    }

    public CancellationQuote quoteCancellation(long reservationId) {
        Reservation r = get(reservationId);
        Money paid = payments.amountPaid(reservationId);
        LocalDateTime now = now();
        return new CancellationQuote(r, paid, refundPolicy.refundFor(r, paid, now), refundPolicy.explain(r, now));
    }

    /** Cancels a reservation, frees the seat, and refunds according to the refund policy. */
    public CancellationQuote cancel(long reservationId) {
        CancellationQuote quote = tx.inTransaction(() -> {
            CancellationQuote q = quoteCancellation(reservationId);
            Reservation r = q.reservation();
            r.cancel(now());
            reservations.update(r);
            payments.refund(reservationId, q.refund());
            return q;
        });
        events.publish(EventBus.Topic.RESERVATIONS);
        events.publish(EventBus.Topic.FLIGHTS);
        return quote;
    }

    // ------------------------------------------------------------------ modify

    /** Price of moving a reservation; a positive difference must be paid, a negative one is refunded. */
    public record ModificationQuote(Reservation reservation, Flight newFlight, Seat newSeat, FareQuote newFare,
                                    Money amountPaid, Money difference) {
        public boolean requiresPayment() {
            return difference.isPositive();
        }
    }

    public ModificationQuote quoteModification(long reservationId, long newFlightId, String newSeatCode) {
        Reservation r = get(reservationId);
        Flight newFlight = flights.findById(newFlightId).orElseThrow(() -> new NotFoundException("Flight", newFlightId));
        Seat newSeat = newFlight.aircraft().requireSeat(newSeatCode);
        boolean sameFlight = Objects.equals(newFlight.id(), r.flight().id());
        if (sameFlight && newSeat.code().equals(r.seat().code())) {
            throw new ValidationException("Choose a different flight or seat.");
        }
        if (flights.occupiedSeats(newFlightId).contains(newSeat.code())) {
            throw seatTaken(newSeat);
        }
        FareQuote newFare = sameFlight && newSeat.seatClass() == r.seat().seatClass()
                ? r.fare()   // seat swap in the same cabin keeps the original price
                : quote(newFlight, newSeat.seatClass());
        Money paid = payments.amountPaid(reservationId);
        return new ModificationQuote(r, newFlight, newSeat, newFare, paid, newFare.total().minus(paid));
    }

    /**
     * Moves a confirmed reservation to another seat and/or flight on the same route.
     * A fare increase is charged to {@code card} first; if the change then fails, the charge is refunded.
     * A fare decrease is refunded to the original card.
     */
    public Reservation modify(long reservationId, long newFlightId, String newSeatCode, CardDetails card) {
        ModificationQuote q = quoteModification(reservationId, newFlightId, newSeatCode);
        q.reservation().status().state().checkModifiable(q.reservation());

        Payment extraCharge = null;
        if (q.requiresPayment()) {
            if (card == null) {
                throw new ValidationException("Payment details are required to cover the fare difference of "
                        + q.difference() + ".");
            }
            extraCharge = payments.charge(reservationId, card, q.difference());
            if (extraCharge.status() != PaymentStatus.COMPLETED) {
                throw new PaymentDeclinedException(extraCharge.failureReason());
            }
        }
        try {
            tx.inTransaction(() -> {
                Reservation fresh = get(reservationId);
                fresh.modify(q.newFlight(), q.newSeat(), q.newFare(), now());
                reservations.update(fresh);
                if (q.difference().isNegative()) {
                    payments.refund(reservationId, q.difference().abs());
                }
            });
        } catch (RuntimeException e) {
            if (extraCharge != null) {
                payments.refund(reservationId, q.difference());
            }
            if (e instanceof DuplicateKeyException) {
                throw seatTaken(q.newSeat());
            }
            throw e;
        }
        events.publish(EventBus.Topic.RESERVATIONS);
        events.publish(EventBus.Topic.FLIGHTS);
        return get(reservationId);
    }

    // ------------------------------------------------------------------ view

    public Reservation get(long reservationId) {
        Reservation r = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation", reservationId));
        guard.requireReservationAccess(r);
        return r;
    }

    /** The signed-in customer's reservations, newest departure first. */
    public List<Reservation> myReservations() {
        session.require(Permission.VIEW_OWN_RESERVATIONS);
        return reservations.findByCustomer(session.customerId().orElseThrow());
    }

    public List<Reservation> search(String text, ReservationStatus status) {
        session.require(Permission.MANAGE_ALL_RESERVATIONS);
        return reservations.search(text, status);
    }

    public BookingConfirmation confirmation(long reservationId) {
        Reservation r = get(reservationId);
        if (r.status() == ReservationStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("A confirmation is available once payment is complete.");
        }
        List<Payment> history = paymentRecords.findByReservation(reservationId);
        return new BookingConfirmation(r, history, payments.amountPaid(reservationId), now());
    }

    /** Writes the confirmation to a file in the renderer's format (Strategy). */
    public void exportConfirmation(long reservationId, Path target, ConfirmationRenderer renderer) {
        exporter.write(target, renderer.render(confirmation(reservationId)));
    }

    // ------------------------------------------------------------------ housekeeping

    public record HousekeepingResult(int expiredHolds, int completedTrips) {
    }

    /**
     * Run at start-up and periodically: releases abandoned holds and marks flown trips as completed.
     * Runs as the system, so it does not require a signed-in user.
     */
    public HousekeepingResult runHousekeeping() {
        LocalDateTime now = now();
        int expired = 0;
        for (Reservation r : reservations.findPendingCreatedBefore(now.minus(HOLD_EXPIRY))) {
            tx.inTransaction(() -> {
                r.cancel(now);
                reservations.update(r);
                payments.refund(r.id(), payments.amountPaid(r.id()));
            });
            expired++;
        }
        int completed = 0;
        for (Reservation r : reservations.findConfirmedDepartedBefore(now)) {
            r.complete(now);
            reservations.update(r);
            completed++;
        }
        if (expired + completed > 0) {
            events.publish(EventBus.Topic.RESERVATIONS);
        }
        return new HousekeepingResult(expired, completed);
    }

    // ------------------------------------------------------------------ helpers

    private String uniqueCode() {
        for (int i = 0; i < CODE_ATTEMPTS; i++) {
            String code = codes.next();
            if (!reservations.confirmationCodeExists(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique confirmation code");
    }

    private static BusinessRuleException seatTaken(Seat seat) {
        return new BusinessRuleException("Seat " + seat.code() + " is no longer available. Please choose another seat.");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
