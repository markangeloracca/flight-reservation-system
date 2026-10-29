package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Airline;
import ca.ucalgary.frs.domain.model.Airport;
import ca.ucalgary.frs.domain.repository.ReferenceDataRepository;

import java.util.List;
import java.util.Optional;

public final class SqliteReferenceDataRepository implements ReferenceDataRepository {
    private final Database db;

    public SqliteReferenceDataRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<Airport> findAllAirports() {
        return db.withConnection(c -> Sql.list(c, "SELECT code, name, city, country FROM airports ORDER BY city",
                rs -> Mappers.airport(rs, "")));
    }

    @Override
    public Optional<Airport> findAirport(String code) {
        return db.withConnection(c -> Sql.one(c, "SELECT code, name, city, country FROM airports WHERE code = ?",
                rs -> Mappers.airport(rs, ""), code));
    }

    @Override
    public List<Airline> findAllAirlines() {
        return db.withConnection(c -> Sql.list(c, "SELECT code, name FROM airlines ORDER BY name",
                rs -> new Airline(rs.getString("code"), rs.getString("name"))));
    }

    @Override
    public Optional<Airline> findAirline(String code) {
        return db.withConnection(c -> Sql.one(c, "SELECT code, name FROM airlines WHERE code = ?",
                rs -> new Airline(rs.getString("code"), rs.getString("name")), code));
    }
}
