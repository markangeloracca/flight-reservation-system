package ca.ucalgary.frs.domain.repository;

import ca.ucalgary.frs.domain.model.Airline;
import ca.ucalgary.frs.domain.model.Airport;

import java.util.List;
import java.util.Optional;

/** Read-only lookup data: airports and airlines. */
public interface ReferenceDataRepository {
    List<Airport> findAllAirports();

    Optional<Airport> findAirport(String code);

    List<Airline> findAllAirlines();

    Optional<Airline> findAirline(String code);
}
