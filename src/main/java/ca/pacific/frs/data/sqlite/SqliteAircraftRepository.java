package ca.pacific.frs.data.sqlite;

import ca.pacific.frs.data.db.Database;
import ca.pacific.frs.data.db.Sql;
import ca.pacific.frs.domain.model.Aircraft;
import ca.pacific.frs.domain.repository.AircraftRepository;

import java.util.List;
import java.util.Optional;

public final class SqliteAircraftRepository implements AircraftRepository {
    private static final String SELECT =
            "SELECT id, registration, model, business_rows, economy_rows, seats_per_row FROM aircraft ";

    private final Database db;

    public SqliteAircraftRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<Aircraft> findAll() {
        return db.withConnection(c -> Sql.list(c, SELECT + "ORDER BY model, registration",
                rs -> Mappers.aircraft(rs, "id")));
    }

    @Override
    public Optional<Aircraft> findById(long id) {
        return db.withConnection(c -> Sql.one(c, SELECT + "WHERE id = ?", rs -> Mappers.aircraft(rs, "id"), id));
    }

    @Override
    public boolean registrationExists(String registration, Long excludingId) {
        return db.withConnection(c -> Sql.exists(c,
                "SELECT 1 FROM aircraft WHERE registration = ? AND id <> IFNULL(?, -1)", registration, excludingId));
    }

    @Override
    public Aircraft save(Aircraft a) {
        return db.withConnection(c -> {
            if (a.id() == null) {
                long id = Sql.insert(c, """
                        INSERT INTO aircraft (registration, model, business_rows, economy_rows, seats_per_row)
                        VALUES (?, ?, ?, ?, ?)
                        """, a.registration(), a.model(), a.businessRows(), a.economyRows(), a.seatsPerRow());
                return a.withId(id);
            }
            Sql.update(c, """
                    UPDATE aircraft SET registration = ?, model = ?, business_rows = ?, economy_rows = ?, seats_per_row = ?
                    WHERE id = ?
                    """, a.registration(), a.model(), a.businessRows(), a.economyRows(), a.seatsPerRow(), a.id());
            return a;
        });
    }

    @Override
    public void delete(long id) {
        db.withConnection(c -> Sql.update(c, "DELETE FROM aircraft WHERE id = ?", id));
    }

    @Override
    public boolean isInUse(long id) {
        return db.withConnection(c -> Sql.exists(c, "SELECT 1 FROM flights WHERE aircraft_id = ? LIMIT 1", id));
    }
}
