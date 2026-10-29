package ca.ucalgary.frs.domain.repository;

import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> findById(long id);

    boolean confirmationCodeExists(String code);

    List<Reservation> findByCustomer(long customerId);

    /** Matches confirmation code, customer name/email or flight number; optional status filter. */
    List<Reservation> search(String text, ReservationStatus status);

    List<Reservation> findActiveByFlight(long flightId);

    /** Live reservations on future flights flown by an aircraft (used before changing its seat layout). */
    List<Reservation> findActiveByAircraft(long aircraftId);

    List<Reservation> findPendingCreatedBefore(LocalDateTime cutoff);

    List<Reservation> findConfirmedDepartedBefore(LocalDateTime now);
}
