package ca.ucalgary.frs.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** The three user roles from the project brief, each with its set of permissions. */
public enum Role {
    CUSTOMER("Customer", EnumSet.of(
            Permission.SEARCH_FLIGHTS,
            Permission.BOOK_FLIGHTS,
            Permission.VIEW_OWN_RESERVATIONS,
            Permission.EDIT_OWN_PROFILE)),
    AGENT("Flight Agent", EnumSet.of(
            Permission.SEARCH_FLIGHTS,
            Permission.BOOK_FLIGHTS,
            Permission.MANAGE_CUSTOMERS,
            Permission.MANAGE_ALL_RESERVATIONS,
            Permission.VIEW_SCHEDULES)),
    ADMIN("System Administrator", EnumSet.of(
            Permission.SEARCH_FLIGHTS,
            Permission.VIEW_SCHEDULES,
            Permission.MANAGE_FLIGHTS,
            Permission.MANAGE_ROUTES_AND_AIRCRAFT));

    private final String displayName;
    private final Set<Permission> permissions;

    Role(String displayName, Set<Permission> permissions) {
        this.displayName = displayName;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public String displayName() {
        return displayName;
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
