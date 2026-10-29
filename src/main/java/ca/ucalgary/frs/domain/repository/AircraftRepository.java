package ca.ucalgary.frs.domain.repository;

import ca.ucalgary.frs.domain.model.Aircraft;

import java.util.List;
import java.util.Optional;

public interface AircraftRepository {
    List<Aircraft> findAll();

    Optional<Aircraft> findById(long id);

    boolean registrationExists(String registration, Long excludingId);

    Aircraft save(Aircraft aircraft);

    void delete(long id);

    boolean isInUse(long id);
}
