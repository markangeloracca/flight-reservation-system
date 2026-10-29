package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.exception.NotFoundException;
import ca.ucalgary.frs.domain.model.Airline;
import ca.ucalgary.frs.domain.model.Airport;
import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.Permission;
import ca.ucalgary.frs.domain.model.SeatMap;
import ca.ucalgary.frs.domain.repository.FlightQueryRepository;
import ca.ucalgary.frs.domain.repository.FlightSearchCriteria;
import ca.ucalgary.frs.domain.repository.ReferenceDataRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Use cases: Search for flights, View flight details and seat availability, View flight schedules. */
public final class FlightSearchController {
    private final FlightQueryRepository flights;
    private final ReferenceDataRepository referenceData;
    private final Session session;
    private final Clock clock;

    public FlightSearchController(FlightQueryRepository flights, ReferenceDataRepository referenceData,
                                  Session session, Clock clock) {
        this.flights = flights;
        this.referenceData = referenceData;
        this.session = session;
        this.clock = clock;
    }

    public List<Airport> airports() {
        return referenceData.findAllAirports();
    }

    public List<Airline> airlines() {
        return referenceData.findAllAirlines();
    }

    /** Bookable flights matching the filters. Any argument may be null for "any". */
    public List<FlightAvailability> search(String originCode, String destinationCode, LocalDate date, String airlineCode) {
        session.require(Permission.SEARCH_FLIGHTS);
        return flights.search(FlightSearchCriteria.builder()
                .origin(originCode)
                .destination(destinationCode)
                .date(date)
                .airline(airlineCode)
                .departingAfter(now())
                .bookableOnly(true)
                .build());
    }

    /** Upcoming bookable flights on the same route as {@code flight}, used when changing a reservation. */
    public List<FlightAvailability> alternativesTo(Flight flight) {
        session.requireUser();
        return flights.search(FlightSearchCriteria.builder()
                .origin(flight.route().origin().code())
                .destination(flight.route().destination().code())
                .departingAfter(now())
                .bookableOnly(true)
                .build());
    }

    public Flight flight(long flightId) {
        session.requireUser();
        return flights.findById(flightId).orElseThrow(() -> new NotFoundException("Flight", flightId));
    }

    public SeatMap seatMap(long flightId) {
        return new SeatMap(flight(flightId), flights.occupiedSeats(flightId));
    }

    /** All flights (any status) departing between the two dates, inclusive. */
    public List<FlightAvailability> schedule(LocalDate from, LocalDate to) {
        session.require(Permission.VIEW_SCHEDULES);
        return flights.schedule(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
