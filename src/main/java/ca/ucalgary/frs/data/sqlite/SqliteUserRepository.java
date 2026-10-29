package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Role;
import ca.ucalgary.frs.domain.model.UserFactory;
import ca.ucalgary.frs.domain.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public final class SqliteUserRepository implements UserRepository {
    private final Database db;

    public SqliteUserRepository(Database db) {
        this.db = db;
    }

    @Override
    public Optional<StoredCredentials> findCredentials(String username) {
        return db.withConnection(c -> Sql.one(c, """
                SELECT id, username, password_hash, role, full_name, email, customer_id
                FROM users WHERE username = ?
                """, rs -> new StoredCredentials(
                UserFactory.create(Role.valueOf(rs.getString("role")), rs.getLong("id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("email"), Sql.nullableLong(rs, "customer_id")),
                rs.getString("password_hash")), username));
    }

    @Override
    public boolean usernameExists(String username) {
        return db.withConnection(c -> Sql.exists(c, "SELECT 1 FROM users WHERE username = ?", username));
    }

    @Override
    public Optional<Long> findUserIdByCustomer(long customerId) {
        return db.withConnection(c -> Sql.one(c, "SELECT id FROM users WHERE customer_id = ?",
                rs -> rs.getLong(1), customerId));
    }

    @Override
    public long create(String username, String passwordHash, Role role, String fullName, String email, Long customerId) {
        return db.withConnection(c -> Sql.insert(c, """
                INSERT INTO users (username, password_hash, role, full_name, email, customer_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, username, passwordHash, role, fullName, email, customerId, LocalDateTime.now().withNano(0)));
    }

    @Override
    public void updateProfile(long userId, String fullName, String email) {
        db.withConnection(c -> Sql.update(c, "UPDATE users SET full_name = ?, email = ? WHERE id = ?",
                fullName, email, userId));
    }
}
