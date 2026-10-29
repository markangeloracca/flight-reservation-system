package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.repository.FlightRepository;
import ca.ucalgary.frs.domain.repository.FlightSearchCriteria;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SqliteFlightRepository implements FlightRepository {
    private static final String SELECT_FLIGHT = "SELECT " + Mappers.FLIGHT_COLUMNS + " FROM flights f " + Mappers.FLIGHT_JOINS;
    private static final String SELECT_AVAILABILITY = "SELECT " + Mappers.FLIGHT_COLUMNS + ", "
            + Mappers.AVAILABILITY_COLUMNS + " FROM flights f " + Mappers.FLIGHT_JOINS;
    private static final int MAX_RESULTS = 500;

    private final Database db;

    public SqliteFlightRepository(Database db) {
        this.db = db;
    }

    @Override
    public Optional<Flight> findById(long id) {
        return db.withConnection(c -> Sql.one(c, SELECT_FLIGHT + "WHERE f.id = ?", Mappers::flight, id));
    }

    @Override
    public List<FlightAvailability> search(FlightSearchCriteria q) {
        String dateFrom = q.dateFrom() == null ? null : q.dateFrom().toString();
        String dateTo = q.dateTo() == null ? null : q.dateTo().toString();
        return db.withConnection(c -> Sql.list(c, SELECT_AVAILABILITY + """
                WHERE (? IS NULL OR r.origin = ?)
                  AND (? IS NULL OR r.destination = ?)
                  AND (? IS NULL OR f.airline_code = ?)
                  AND (? IS NULL OR substr(f.departure_time, 1, 10) >= ?)
                  AND (? IS NULL OR substr(f.departure_time, 1, 10) <= ?)
                  AND (? IS NULL OR f.departure_time > ?)
                  AND (? = 0 OR f.status <> 'CANCELLED')
                ORDER BY f.departure_time
                LIMIT ?
                """, Mappers::availability,
                q.originCode(), q.originCode(),
                q.destinationCode(), q.destinationCode(),
                q.airlineCode(), q.airlineCode(),
                dateFrom, dateFrom,
                dateTo, dateTo,
                q.departingAfter(), q.departingAfter(),
                q.bookableOnly(),
                MAX_RESULTS));
    }

    @Override
    public List<FlightAvailability> schedule(LocalDateTime from, LocalDateTime to) {
        return db.withConnection(c -> Sql.list(c, SELECT_AVAILABILITY + """
                WHERE f.departure_time >= ? AND f.departure_time < ?
                ORDER BY f.departure_time
                """, Mappers::availability, from, to));
    }

    @Override
    public Set<String> occupiedSeats(long flightId) {
        return db.withConnection(c -> new HashSet<>(Sql.list(c, """
                SELECT seat_code FROM reservations
                WHERE flight_id = ? AND status IN ('PENDING_PAYMENT', 'CONFIRMED')
                """, rs -> rs.getString(1), flightId)));
    }

    @Override
    public Flight save(Flight f) {
        return db.withConnection(c -> {
            if (f.id() == null) {
                long id = Sql.insert(c, """
                        INSERT INTO flights (flight_number, airline_code, route_id, aircraft_id, departure_time,
                                             arrival_time, base_fare_cents, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, f.flightNumber(), f.airline().code(), f.route().id(), f.aircraft().id(),
                        f.departureTime(), f.arrivalTime(), f.baseFare(), f.status());
                return f.toBuilder().id(id).build();
            }
            Sql.update(c, """
                    UPDATE flights SET flight_number = ?, airline_code = ?, route_id = ?, aircraft_id = ?,
                           departure_time = ?, arrival_time = ?, base_fare_cents = ?, status = ?
                    WHERE id = ?
                    """, f.flightNumber(), f.airline().code(), f.route().id(), f.aircraft().id(),
                    f.departureTime(), f.arrivalTime(), f.baseFare(), f.status(), f.id());
            return f;
        });
    }

    @Override
    public void delete(long id) {
        db.withConnection(c -> Sql.update(c, "DELETE FROM flights WHERE id = ?", id));
    }

    @Override
    public boolean hasReservations(long flightId) {
        return db.withConnection(c -> Sql.exists(c, "SELECT 1 FROM reservations WHERE flight_id = ? LIMIT 1", flightId));
    }

    @Override
    public boolean flightNumberTaken(String flightNumber, LocalDateTime departure, Long excludingId) {
        return db.withConnection(c -> Sql.exists(c, """
                SELECT 1 FROM flights WHERE flight_number = ? AND departure_time = ? AND id <> IFNULL(?, -1)
                """, flightNumber, departure, excludingId));
    }

    @Override
    public int deleteDepartedWithoutReservations(LocalDateTime cutoff) {
        return db.withConnection(c -> Sql.update(c, """
                DELETE FROM flights
                WHERE departure_time < ?
                  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.flight_id = flights.id)
                """, cutoff));
    }
}
