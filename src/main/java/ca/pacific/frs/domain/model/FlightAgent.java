package ca.pacific.frs.domain.model;

/** An employee who manages customers and their reservations. */
public final class FlightAgent extends User {
    public FlightAgent(long id, String username, String fullName, String email) {
        super(id, username, fullName, email);
    }

    @Override
    public Role role() {
        return Role.AGENT;
    }
}
