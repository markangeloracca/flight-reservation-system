package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.ReservationStatus;
import ca.ucalgary.frs.domain.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class SqliteReservationRepository implements ReservationRepository {
    private static final String SELECT = """
            SELECT res.id AS res_id, res.confirmation_code, res.seat_code, res.seat_class, res.fare_cents,
                   res.discount_cents, res.tax_cents, res.total_cents, res.promotion_title, res.status AS res_status,
                   res.booked_by_user_id, res.created_at AS res_created_at, res.updated_at AS res_updated_at,
            """ + Mappers.CUSTOMER_COLUMNS + ", " + Mappers.FLIGHT_COLUMNS + """
            FROM reservations res
            JOIN customers c ON c.id = res.customer_id
            JOIN flights f ON f.id = res.flight_id
            """ + Mappers.FLIGHT_JOINS;

    private final Database db;

    public SqliteReservationRepository(Database db) {
        this.db = db;
    }

    @Override
    public Reservation insert(Reservation r) {
        return db.withConnection(c -> {
            long id = Sql.insert(c, """
                    INSERT INTO reservations (confirmation_code, customer_id, flight_id, seat_code, seat_class,
                        fare_cents, discount_cents, tax_cents, total_cents, promotion_title, status,
                        booked_by_user_id, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, r.confirmationCode(), r.customer().id(), r.flight().id(), r.seat().code(),
                    r.seat().seatClass(), r.fare().fare(), r.fare().discount(), r.fare().tax(), r.fare().total(),
                    r.fare().promotionTitle(), r.status(), r.bookedByUserId(), r.createdAt(), r.updatedAt());
            return r.withId(id);
        });
    }

    @Override
    public void update(Reservation r) {
        db.withConnection(c -> Sql.update(c, """
                UPDATE reservations SET flight_id = ?, seat_code = ?, seat_class = ?, fare_cents = ?,
                    discount_cents = ?, tax_cents = ?, total_cents = ?, promotion_title = ?, status = ?, updated_at = ?
                WHERE id = ?
                """, r.flight().id(), r.seat().code(), r.seat().seatClass(), r.fare().fare(), r.fare().discount(),
                r.fare().tax(), r.fare().total(), r.fare().promotionTitle(), r.status(), r.updatedAt(), r.id()));
    }

    @Override
    public Optional<Reservation> findById(long id) {
        return db.withConnection(c -> Sql.one(c, SELECT + "WHERE res.id = ?", Mappers::reservation, id));
    }

    @Override
    public boolean confirmationCodeExists(String code) {
        return db.withConnection(c -> Sql.exists(c, "SELECT 1 FROM reservations WHERE confirmation_code = ?", code));
    }

    @Override
    public List<Reservation> findByCustomer(long customerId) {
        return db.withConnection(c -> Sql.list(c, SELECT + "WHERE res.customer_id = ? ORDER BY f.departure_time DESC",
                Mappers::reservation, customerId));
    }

    @Override
    public List<Reservation> search(String text, ReservationStatus status) {
        String like = text == null || text.isBlank() ? null : Sql.likeContains(text);
        return db.withConnection(c -> Sql.list(c, SELECT + """
                WHERE (? IS NULL OR res.status = ?)
                  AND (? IS NULL
                       OR res.confirmation_code LIKE ? ESCAPE '\\'
                       OR (c.first_name || ' ' || c.last_name) LIKE ? ESCAPE '\\'
                       OR c.email LIKE ? ESCAPE '\\'
                       OR f.flight_number LIKE ? ESCAPE '\\')
                ORDER BY f.departure_time DESC
                LIMIT 1000
                """, Mappers::reservation, status, status, like, like, like, like, like));
    }

    @Override
    public List<Reservation> findActiveByFlight(long flightId) {
        return db.withConnection(c -> Sql.list(c, SELECT + """
                WHERE res.flight_id = ? AND res.status IN ('PENDING_PAYMENT', 'CONFIRMED')
                ORDER BY res.seat_code
                """, Mappers::reservation, flightId));
    }

    @Override
    public List<Reservation> findActiveByAircraft(long aircraftId) {
        return db.withConnection(c -> Sql.list(c, SELECT + """
                WHERE f.aircraft_id = ? AND res.status IN ('PENDING_PAYMENT', 'CONFIRMED')
                """, Mappers::reservation, aircraftId));
    }

    @Override
    public List<Reservation> findPendingCreatedBefore(LocalDateTime cutoff) {
        return db.withConnection(c -> Sql.list(c, SELECT + """
                WHERE res.status = 'PENDING_PAYMENT' AND res.created_at < ?
                """, Mappers::reservation, cutoff));
    }

    @Override
    public List<Reservation> findConfirmedDepartedBefore(LocalDateTime now) {
        return db.withConnection(c -> Sql.list(c, SELECT + """
                WHERE res.status = 'CONFIRMED' AND f.departure_time <= ?
                """, Mappers::reservation, now));
    }
}
