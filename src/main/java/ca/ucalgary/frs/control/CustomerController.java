package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.exception.NotFoundException;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.domain.model.Payment;
import ca.ucalgary.frs.domain.model.Permission;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.repository.CustomerRepository;
import ca.ucalgary.frs.domain.repository.TransactionManager;
import ca.ucalgary.frs.domain.repository.PaymentRepository;
import ca.ucalgary.frs.domain.repository.ReservationRepository;
import ca.ucalgary.frs.domain.repository.UserRepository;

import java.util.List;

/** Use cases: Manage customer profiles (agent), View and edit own profile (customer), View booking history. */
public final class CustomerController {
    private final TransactionManager db;
    private final CustomerRepository customers;
    private final UserRepository users;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final Session session;
    private final EventBus events;
    private final AccessGuard guard;

    public CustomerController(TransactionManager db, CustomerRepository customers, UserRepository users,
                              ReservationRepository reservations, PaymentRepository payments,
                              Session session, EventBus events) {
        this.db = db;
        this.customers = customers;
        this.users = users;
        this.reservations = reservations;
        this.payments = payments;
        this.session = session;
        this.events = events;
        this.guard = new AccessGuard(session);
    }

    public List<Customer> search(String text) {
        session.require(Permission.MANAGE_CUSTOMERS);
        return customers.search(text);
    }

    public Customer get(long customerId) {
        guard.requireCustomerAccess(customerId);
        return customers.findById(customerId).orElseThrow(() -> new NotFoundException("Customer", customerId));
    }

    /** The signed-in customer's own profile. */
    public Customer myProfile() {
        session.require(Permission.EDIT_OWN_PROFILE);
        long id = session.customerId().orElseThrow();
        return get(id);
    }

    public Customer create(Customer customer) {
        session.require(Permission.MANAGE_CUSTOMERS);
        if (customer.id() != null) {
            throw new IllegalArgumentException("New customer must not have an id");
        }
        Customer saved = db.inTransaction(() -> {
            ensureEmailFree(customer.email(), null);
            return customers.save(customer);
        });
        events.publish(EventBus.Topic.CUSTOMERS);
        return saved;
    }

    /** Agents may edit anyone; customers only themselves. Keeps the linked sign-in account in sync. */
    public Customer update(Customer customer) {
        if (customer.id() == null) {
            throw new IllegalArgumentException("Customer id required");
        }
        guard.requireCustomerAccess(customer.id());
        Customer saved = db.inTransaction(() -> {
            customers.findById(customer.id()).orElseThrow(() -> new NotFoundException("Customer", customer.id()));
            ensureEmailFree(customer.email(), customer.id());
            Customer s = customers.save(customer);
            users.findUserIdByCustomer(s.id()).ifPresent(uid -> users.updateProfile(uid, s.fullName(), s.email()));
            return s;
        });
        events.publish(EventBus.Topic.CUSTOMERS);
        events.publish(EventBus.Topic.RESERVATIONS);
        return saved;
    }

    public List<Reservation> reservations(long customerId) {
        guard.requireCustomerAccess(customerId);
        return reservations.findByCustomer(customerId);
    }

    public List<Payment> payments(long customerId) {
        guard.requireCustomerAccess(customerId);
        return payments.findByCustomer(customerId);
    }

    public boolean hasAccount(long customerId) {
        session.require(Permission.MANAGE_CUSTOMERS);
        return users.findUserIdByCustomer(customerId).isPresent();
    }

    private void ensureEmailFree(String email, Long selfId) {
        customers.findByEmail(email)
                .filter(other -> !other.id().equals(selfId))
                .ifPresent(other -> {
                    throw new ValidationException("email", "Another customer already uses this email address.");
                });
    }
}
