package ca.pacific.frs.domain.repository;

import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.FlightAvailability;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Read side of flight storage (Interface Segregation: search screens need only this). */
public interface FlightQueryRepository {
    Optional<Flight> findById(long id);

    List<FlightAvailability> search(FlightSearchCriteria criteria);

    /** Flights departing in [from, to), with availability, ordered by departure. */
    List<FlightAvailability> schedule(LocalDateTime from, LocalDateTime to);

    /** Seat codes held by pending or confirmed reservations on a flight. */
    Set<String> occupiedSeats(long flightId);
}
