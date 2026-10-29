package ca.pacific.frs.domain.repository;

import ca.pacific.frs.domain.model.Route;

import java.util.List;
import java.util.Optional;

public interface RouteRepository {
    List<Route> findAll();

    Optional<Route> findById(long id);

    boolean exists(String originCode, String destinationCode, Long excludingId);

    Route save(Route route);

    void delete(long id);

    boolean isInUse(long id);
}
