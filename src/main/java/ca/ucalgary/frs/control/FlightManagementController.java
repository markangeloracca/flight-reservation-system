package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.exception.BusinessRuleException;
import ca.ucalgary.frs.domain.exception.NotFoundException;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Aircraft;
import ca.ucalgary.frs.domain.model.Airline;
import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.FlightStatus;
import ca.ucalgary.frs.domain.model.Permission;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.Route;
import ca.ucalgary.frs.domain.repository.AircraftRepository;
import ca.ucalgary.frs.domain.repository.FlightRepository;
import ca.ucalgary.frs.domain.repository.ReferenceDataRepository;
import ca.ucalgary.frs.domain.repository.ReservationRepository;
import ca.ucalgary.frs.domain.repository.RouteRepository;
import ca.ucalgary.frs.domain.repository.TransactionManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Administrator use cases: Add flight, Update flight / schedule, Cancel flight, Remove outdated flights. */
public final class FlightManagementController {
    private final TransactionManager tx;
    private final FlightRepository flights;
    private final RouteRepository routes;
    private final AircraftRepository aircraft;
    private final ReferenceDataRepository referenceData;
    private final ReservationRepository reservations;
    private final PaymentController payments;
    private final Session session;
    private final EventBus events;
    private final Clock clock;

    public FlightManagementController(TransactionManager tx, FlightRepository flights, RouteRepository routes,
                                      AircraftRepository aircraft, ReferenceDataRepository referenceData,
                                      ReservationRepository reservations, PaymentController payments,
                                      Session session, EventBus events, Clock clock) {
        this.tx = tx;
        this.flights = flights;
        this.routes = routes;
        this.aircraft = aircraft;
        this.referenceData = referenceData;
        this.reservations = reservations;
        this.payments = payments;
        this.session = session;
        this.events = events;
        this.clock = clock;
    }

    public List<FlightAvailability> flights(LocalDate from, LocalDate to) {
        session.require(Permission.MANAGE_FLIGHTS);
        return flights.schedule(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public List<Airline> airlines() {
        return referenceData.findAllAirlines();
    }

    public List<Route> routes() {
        return routes.findAll();
    }

    public List<Aircraft> aircraft() {
        return aircraft.findAll();
    }

    /**
     * Adds or updates a flight. For a flight that already has bookings, the route cannot change and a
     * new aircraft must still contain every booked seat in the same cabin.
     */
    public Flight save(Flight flight) {
        session.require(Permission.MANAGE_FLIGHTS);
        if (flight.status() == FlightStatus.CANCELLED) {
            throw new ValidationException("status", "Use \"Cancel flight\" to cancel a flight so passengers are refunded.");
        }
        Flight saved = tx.inTransaction(() -> {
            if (flights.flightNumberTaken(flight.flightNumber(), flight.departureTime(), flight.id())) {
                throw new ValidationException("flightNumber",
                        flight.flightNumber() + " already departs at that time.");
            }
            if (flight.id() == null) {
                if (flight.departureTime().isBefore(now())) {
                    throw new ValidationException("departureTime", "A new flight must depart in the future.");
                }
            } else {
                Flight existing = flights.findById(flight.id())
                        .orElseThrow(() -> new NotFoundException("Flight", flight.id()));
                if (existing.status() == FlightStatus.CANCELLED) {
                    throw new BusinessRuleException("A cancelled flight cannot be edited.");
                }
                List<Reservation> booked = reservations.findActiveByFlight(flight.id());
                if (!booked.isEmpty() && !Objects.equals(existing.route().id(), flight.route().id())) {
                    throw new ValidationException("route", "This flight has bookings, so its route cannot change.");
                }
                checkSeatsFit(flight.aircraft(), booked);
            }
            return flights.save(flight);
        });
        events.publish(EventBus.Topic.FLIGHTS);
        events.publish(EventBus.Topic.RESERVATIONS);
        return saved;
    }

    /** Cancels a flight and every live reservation on it, refunding passengers in full. Returns the count. */
    public int cancelFlight(long flightId) {
        session.require(Permission.MANAGE_FLIGHTS);
        int affected = tx.inTransaction(() -> {
            Flight f = flights.findById(flightId).orElseThrow(() -> new NotFoundException("Flight", flightId));
            if (f.hasDeparted(now())) {
                throw new BusinessRuleException("A flight that has departed cannot be cancelled.");
            }
            List<Reservation> live = reservations.findActiveByFlight(flightId);
            for (Reservation r : live) {
                r.cancel(now());
                reservations.update(r);
                payments.refund(r.id(), payments.amountPaid(r.id()));
            }
            flights.save(f.toBuilder().status(FlightStatus.CANCELLED).build());
            return live.size();
        });
        events.publish(EventBus.Topic.FLIGHTS);
        events.publish(EventBus.Topic.RESERVATIONS);
        return affected;
    }

    /** Deletes a flight that has never been booked. Booked flights must be cancelled instead. */
    public void deleteFlight(long flightId) {
        session.require(Permission.MANAGE_FLIGHTS);
        tx.inTransaction(() -> {
            if (flights.hasReservations(flightId)) {
                throw new BusinessRuleException(
                        "This flight has booking history and cannot be deleted. Cancel it instead.");
            }
            flights.delete(flightId);
        });
        events.publish(EventBus.Topic.FLIGHTS);
    }

    /** Removes departed flights that were never booked. Flights with booking history are kept as records. */
    public int removeOutdatedFlights() {
        session.require(Permission.MANAGE_FLIGHTS);
        int removed = flights.deleteDepartedWithoutReservations(now());
        events.publish(EventBus.Topic.FLIGHTS);
        return removed;
    }

    /** Every live reservation's seat must exist on {@code target} in the same cabin. Shared with aircraft edits. */
    static void checkSeatsFit(Aircraft target, List<Reservation> booked) {
        for (Reservation r : booked) {
            var seat = target.seat(r.seat().code());
            if (seat.isEmpty() || seat.get().seatClass() != r.seat().seatClass()) {
                throw new ValidationException("aircraft", "Booked seat " + r.seat().code() + " (" + r.confirmationCode()
                        + ") would not exist in the same cabin on " + target.model() + ".");
            }
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
