package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Route;
import ca.ucalgary.frs.domain.repository.RouteRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class SqliteRouteRepository implements RouteRepository {
    private static final String SELECT = """
            SELECT r.id AS r_id, r.distance_km,
                   o.code AS o_code, o.name AS o_name, o.city AS o_city, o.country AS o_country,
                   d.code AS d_code, d.name AS d_name, d.city AS d_city, d.country AS d_country
            FROM routes r
            JOIN airports o ON o.code = r.origin
            JOIN airports d ON d.code = r.destination
            """;

    private final Database db;

    public SqliteRouteRepository(Database db) {
        this.db = db;
    }

    private static Route map(ResultSet rs) throws SQLException {
        return new Route(rs.getLong("r_id"), Mappers.airport(rs, "o_"), Mappers.airport(rs, "d_"), rs.getInt("distance_km"));
    }

    @Override
    public List<Route> findAll() {
        return db.withConnection(c -> Sql.list(c, SELECT + "ORDER BY o.city, d.city", SqliteRouteRepository::map));
    }

    @Override
    public Optional<Route> findById(long id) {
        return db.withConnection(c -> Sql.one(c, SELECT + "WHERE r.id = ?", SqliteRouteRepository::map, id));
    }

    @Override
    public boolean exists(String originCode, String destinationCode, Long excludingId) {
        return db.withConnection(c -> Sql.exists(c,
                "SELECT 1 FROM routes WHERE origin = ? AND destination = ? AND id <> IFNULL(?, -1)",
                originCode, destinationCode, excludingId));
    }

    @Override
    public Route save(Route route) {
        return db.withConnection(c -> {
            if (route.id() == null) {
                long id = Sql.insert(c, "INSERT INTO routes (origin, destination, distance_km) VALUES (?, ?, ?)",
                        route.origin().code(), route.destination().code(), route.distanceKm());
                return route.withId(id);
            }
            Sql.update(c, "UPDATE routes SET origin = ?, destination = ?, distance_km = ? WHERE id = ?",
                    route.origin().code(), route.destination().code(), route.distanceKm(), route.id());
            return route;
        });
    }

    @Override
    public void delete(long id) {
        db.withConnection(c -> Sql.update(c, "DELETE FROM routes WHERE id = ?", id));
    }

    @Override
    public boolean isInUse(long id) {
        return db.withConnection(c -> Sql.exists(c, "SELECT 1 FROM flights WHERE route_id = ? LIMIT 1", id));
    }
}
