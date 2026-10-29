package ca.pacific.frs.domain.repository;

import ca.pacific.frs.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(long id);

    Optional<Customer> findByEmail(String email);

    /** Case-insensitive match on name, email or phone. Blank text returns everyone. */
    List<Customer> search(String text);

    /** Inserts when the id is null, otherwise updates. Returns the stored customer. */
    Customer save(Customer customer);
}
