package ca.pacific.frs.domain.model;

/** A customer's sign-in account, linked to their {@link Customer} profile. */
public final class CustomerUser extends User {
    private final long customerId;

    public CustomerUser(long id, String username, String fullName, String email, long customerId) {
        super(id, username, fullName, email);
        this.customerId = customerId;
    }

    public long customerId() {
        return customerId;
    }

    @Override
    public Role role() {
        return Role.CUSTOMER;
    }
}
