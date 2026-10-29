package ca.pacific.frs.domain.model;

/** Fine-grained capabilities. Roles are defined as sets of permissions. */
public enum Permission {
    SEARCH_FLIGHTS,
    BOOK_FLIGHTS,
    VIEW_OWN_RESERVATIONS,
    EDIT_OWN_PROFILE,
    MANAGE_CUSTOMERS,
    MANAGE_ALL_RESERVATIONS,
    VIEW_SCHEDULES,
    MANAGE_FLIGHTS,
    MANAGE_ROUTES_AND_AIRCRAFT
}
