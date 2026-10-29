package ca.ucalgary.frs.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * A person who can sign in. Concrete subclasses define the role; callers use {@link #can(Permission)}
 * rather than checking the subclass, so new roles can be added without changing callers.
 */
public abstract class User {
    private final long id;
    private final String username;
    private final String fullName;
    private final String email;

    protected User(long id, String username, String fullName, String email) {
        this.id = id;
        this.username = Objects.requireNonNull(username);
        this.fullName = Objects.requireNonNull(fullName);
        this.email = email;
    }

    public abstract Role role();

    public Set<Permission> permissions() {
        return role().permissions();
    }

    public final boolean can(Permission permission) {
        return permissions().contains(permission);
    }

    public long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User u && u.id == id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return fullName + " (" + role().displayName() + ")";
    }
}
