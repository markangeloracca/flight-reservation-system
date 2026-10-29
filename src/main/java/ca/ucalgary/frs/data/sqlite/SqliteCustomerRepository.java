package ca.ucalgary.frs.data.sqlite;

import ca.ucalgary.frs.data.db.Database;
import ca.ucalgary.frs.data.db.Sql;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.domain.repository.CustomerRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class SqliteCustomerRepository implements CustomerRepository {
    private static final String SELECT = "SELECT " + Mappers.CUSTOMER_COLUMNS + " FROM customers c ";

    private final Database db;

    public SqliteCustomerRepository(Database db) {
        this.db = db;
    }

    private static Customer map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return Mappers.customer(rs, "c_id", "c_email", "c_phone", "c_created_at");
    }

    @Override
    public Optional<Customer> findById(long id) {
        return db.withConnection(c -> Sql.one(c, SELECT + "WHERE c.id = ?", SqliteCustomerRepository::map, id));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return db.withConnection(c -> Sql.one(c, SELECT + "WHERE c.email = ?", SqliteCustomerRepository::map, email));
    }

    @Override
    public List<Customer> search(String text) {
        if (text == null || text.isBlank()) {
            return db.withConnection(c -> Sql.list(c, SELECT + "ORDER BY c.last_name, c.first_name",
                    SqliteCustomerRepository::map));
        }
        String like = Sql.likeContains(text);
        return db.withConnection(c -> Sql.list(c, SELECT + """
                WHERE (c.first_name || ' ' || c.last_name) LIKE ? ESCAPE '\\'
                   OR c.email LIKE ? ESCAPE '\\'
                   OR IFNULL(c.phone, '') LIKE ? ESCAPE '\\'
                ORDER BY c.last_name, c.first_name
                """, SqliteCustomerRepository::map, like, like, like));
    }

    @Override
    public Customer save(Customer customer) {
        return db.withConnection(c -> {
            if (customer.id() == null) {
                LocalDateTime created = customer.createdAt() != null ? customer.createdAt()
                        : LocalDateTime.now().withNano(0);
                long id = Sql.insert(c, """
                        INSERT INTO customers (first_name, last_name, email, phone, date_of_birth, address, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, customer.firstName(), customer.lastName(), customer.email(), customer.phone(),
                        customer.dateOfBirth(), customer.address(), created);
                return customer.toBuilder().id(id).createdAt(created).build();
            }
            Sql.update(c, """
                    UPDATE customers SET first_name = ?, last_name = ?, email = ?, phone = ?, date_of_birth = ?, address = ?
                    WHERE id = ?
                    """, customer.firstName(), customer.lastName(), customer.email(), customer.phone(),
                    customer.dateOfBirth(), customer.address(), customer.id());
            return customer;
        });
    }
}
