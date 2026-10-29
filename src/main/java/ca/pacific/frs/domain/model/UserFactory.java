package ca.pacific.frs.domain.model;

/** Factory Method: creates the right {@link User} subclass for a stored role. */
public final class UserFactory {
    private UserFactory() {
    }

    public static User create(Role role, long id, String username, String fullName, String email, Long customerId) {
        return switch (role) {
            case CUSTOMER -> {
                if (customerId == null) {
                    throw new IllegalStateException("Customer account " + username + " has no customer profile");
                }
                yield new CustomerUser(id, username, fullName, email, customerId);
            }
            case AGENT -> new FlightAgent(id, username, fullName, email);
            case ADMIN -> new SystemAdministrator(id, username, fullName, email);
        };
    }
}
