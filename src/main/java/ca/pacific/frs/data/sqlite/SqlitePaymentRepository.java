package ca.pacific.frs.data.sqlite;

import ca.pacific.frs.data.db.Database;
import ca.pacific.frs.data.db.Sql;
import ca.pacific.frs.domain.model.Payment;
import ca.pacific.frs.domain.model.PaymentMethodType;
import ca.pacific.frs.domain.model.PaymentStatus;
import ca.pacific.frs.domain.model.PaymentType;
import ca.pacific.frs.domain.repository.PaymentRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqlitePaymentRepository implements PaymentRepository {
    private static final String SELECT = """
            SELECT p.id, p.reservation_id, p.type, p.amount_cents, p.method, p.card_brand, p.card_last4,
                   p.cardholder_name, p.status, p.transaction_ref, p.failure_reason, p.created_at
            FROM payments p
            """;

    private final Database db;

    public SqlitePaymentRepository(Database db) {
        this.db = db;
    }

    private static Payment map(ResultSet rs) throws SQLException {
        return new Payment(rs.getLong("id"), rs.getLong("reservation_id"), PaymentType.valueOf(rs.getString("type")),
                Sql.money(rs, "amount_cents"), PaymentMethodType.valueOf(rs.getString("method")),
                rs.getString("card_brand"), rs.getString("card_last4"), rs.getString("cardholder_name"),
                PaymentStatus.valueOf(rs.getString("status")), rs.getString("transaction_ref"),
                rs.getString("failure_reason"), Sql.timestamp(rs, "created_at"));
    }

    @Override
    public Payment insert(Payment p) {
        return db.withConnection(c -> p.withId(Sql.insert(c, """
                INSERT INTO payments (reservation_id, type, amount_cents, method, card_brand, card_last4,
                    cardholder_name, status, transaction_ref, failure_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, p.reservationId(), p.type(), p.amount(), p.method(), p.cardBrand(), p.cardLast4(),
                p.cardholderName(), p.status(), p.transactionRef(), p.failureReason(), p.createdAt())));
    }

    @Override
    public void update(Payment p) {
        db.withConnection(c -> Sql.update(c,
                "UPDATE payments SET status = ?, transaction_ref = ?, failure_reason = ? WHERE id = ?",
                p.status(), p.transactionRef(), p.failureReason(), p.id()));
    }

    @Override
    public List<Payment> findByReservation(long reservationId) {
        return db.withConnection(c -> Sql.list(c, SELECT + "WHERE p.reservation_id = ? ORDER BY p.created_at, p.id",
                SqlitePaymentRepository::map, reservationId));
    }

    @Override
    public List<Payment> findByCustomer(long customerId) {
        return db.withConnection(c -> Sql.list(c, SELECT + """
                JOIN reservations r ON r.id = p.reservation_id
                WHERE r.customer_id = ?
                ORDER BY p.created_at DESC, p.id DESC
                """, SqlitePaymentRepository::map, customerId));
    }
}
