package ca.ucalgary.frs.domain.repository;

import ca.ucalgary.frs.domain.model.Flight;

import java.time.LocalDateTime;

/** Full flight storage, used by administration. */
public interface FlightRepository extends FlightQueryRepository {
    Flight save(Flight flight);

    void delete(long id);

    boolean hasReservations(long flightId);

    boolean flightNumberTaken(String flightNumber, LocalDateTime departure, Long excludingId);

    /** Deletes flights that departed before {@code cutoff} and were never booked. Returns the count removed. */
    int deleteDepartedWithoutReservations(LocalDateTime cutoff);
}
