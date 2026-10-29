package ca.pacific.frs.domain.model;

import ca.pacific.frs.domain.exception.BusinessRuleException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A booking of one seat on one flight for one customer.
 * Lifecycle rules are delegated to the current {@link ReservationState}.
 */
public final class Reservation {
    private final Long id;
    private final String confirmationCode;
    private final Customer customer;
    private Flight flight;
    private Seat seat;
    private FareQuote fare;
    private ReservationState state;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Long bookedByUserId;

    public Reservation(Long id, String confirmationCode, Customer customer, Flight flight, Seat seat, FareQuote fare,
                       ReservationStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, Long bookedByUserId) {
        this.id = id;
        this.confirmationCode = Objects.requireNonNull(confirmationCode);
        this.customer = Objects.requireNonNull(customer);
        this.flight = Objects.requireNonNull(flight);
        this.seat = Objects.requireNonNull(seat);
        this.fare = Objects.requireNonNull(fare);
        this.state = Objects.requireNonNull(status).state();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.bookedByUserId = bookedByUserId;
    }

    /** Creates a new reservation that holds the seat until payment. */
    public static Reservation hold(String confirmationCode, Customer customer, Flight flight, Seat seat, FareQuote fare,
                                   LocalDateTime now, Long bookedByUserId) {
        if (!flight.isBookable(now)) {
            throw new BusinessRuleException("Flight " + flight.flightNumber() + " can no longer be booked.");
        }
        return new Reservation(null, confirmationCode, customer, flight, seat, fare,
                ReservationStatus.PENDING_PAYMENT, now, now, bookedByUserId);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, confirmationCode, customer, flight, seat, fare, status(), createdAt, updatedAt,
                bookedByUserId);
    }

    // ---- lifecycle (delegated to State) ----

    public void confirm(LocalDateTime now) {
        state.confirm(this);
        touch(now);
    }

    public void cancel(LocalDateTime now) {
        if (flight.hasDeparted(now) && status() == ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("This flight has already departed and can no longer be cancelled.");
        }
        state.cancel(this);
        touch(now);
    }

    public void complete(LocalDateTime now) {
        state.complete(this);
        touch(now);
    }

    /** Moves the booking to another flight and/or seat at a new fare. */
    public void modify(Flight newFlight, Seat newSeat, FareQuote newFare, LocalDateTime now) {
        state.checkModifiable(this);
        if (flight.hasDeparted(now)) {
            throw new BusinessRuleException("This flight has already departed and can no longer be changed.");
        }
        if (!newFlight.isBookable(now)) {
            throw new BusinessRuleException("Flight " + newFlight.flightNumber() + " can no longer be booked.");
        }
        if (!newFlight.route().id().equals(flight.route().id())) {
            throw new BusinessRuleException("A reservation can only be moved to a flight on the same route.");
        }
        this.flight = newFlight;
        this.seat = newSeat;
        this.fare = newFare;
        touch(now);
    }

    void transitionTo(ReservationState next) {
        this.state = next;
    }

    private void touch(LocalDateTime now) {
        this.updatedAt = now;
    }

    // ---- queries ----

    public boolean holdsSeat() {
        return state.holdsSeat();
    }

    public boolean isOwnedBy(long customerId) {
        return customer.id() != null && customer.id() == customerId;
    }

    public ReservationStatus status() {
        return state.status();
    }

    public Long id() {
        return id;
    }

    public String confirmationCode() {
        return confirmationCode;
    }

    public Customer customer() {
        return customer;
    }

    public Flight flight() {
        return flight;
    }

    public Seat seat() {
        return seat;
    }

    public FareQuote fare() {
        return fare;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public Long bookedByUserId() {
        return bookedByUserId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reservation r && id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return confirmationCode + " " + flight.flightNumber() + " " + seat.code() + " " + status();
    }
}
