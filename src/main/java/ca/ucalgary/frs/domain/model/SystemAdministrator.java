package ca.ucalgary.frs.domain.model;

/** Administers flights, routes, aircraft and schedules. */
public final class SystemAdministrator extends User {
    public SystemAdministrator(long id, String username, String fullName, String email) {
        super(id, username, fullName, email);
    }

    @Override
    public Role role() {
        return Role.ADMIN;
    }
}
