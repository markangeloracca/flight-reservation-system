package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Promotion;
import ca.ucalgary.frs.domain.repository.PromotionRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public final class SqlitePromotionRepository implements PromotionRepository {
    private static final String SELECT = "SELECT id, title, description, discount_percent, active FROM promotions ";

    private final Database db;

    public SqlitePromotionRepository(Database db) {
        this.db = db;
    }

    private static Promotion map(ResultSet rs) throws SQLException {
        return new Promotion(rs.getLong("id"), rs.getString("title"), rs.getString("description"),
                rs.getInt("discount_percent"), rs.getInt("active") == 1);
    }

    @Override
    public List<Promotion> findActive() {
        return db.withConnection(c -> Sql.list(c, SELECT + "WHERE active = 1 ORDER BY id", SqlitePromotionRepository::map));
    }

    @Override
    public Optional<Promotion> findById(long id) {
        return db.withConnection(c -> Sql.one(c, SELECT + "WHERE id = ?", SqlitePromotionRepository::map, id));
    }

    @Override
    public boolean hasSeen(long customerId, YearMonth month) {
        return db.withConnection(c -> Sql.exists(c,
                "SELECT 1 FROM promotion_views WHERE customer_id = ? AND month = ?", customerId, month));
    }

    @Override
    public void markSeen(long customerId, YearMonth month) {
        db.withConnection(c -> Sql.update(c,
                "INSERT OR IGNORE INTO promotion_views (customer_id, month) VALUES (?, ?)", customerId, month));
    }
}
