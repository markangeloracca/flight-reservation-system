package ca.pacific.frs.ui.shell;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.domain.model.Permission;
import ca.pacific.frs.domain.model.User;
import ca.pacific.frs.ui.admin.AircraftPage;
import ca.pacific.frs.ui.admin.FlightsPage;
import ca.pacific.frs.ui.admin.RoutesPage;
import ca.pacific.frs.ui.agent.ReservationsPage;
import ca.pacific.frs.ui.agent.SchedulePage;
import ca.pacific.frs.ui.booking.FlightSearchPage;
import ca.pacific.frs.ui.booking.MyTripsPage;
import ca.pacific.frs.ui.customer.CustomersPage;
import ca.pacific.frs.ui.customer.ProfilePage;

import java.util.List;
import java.util.function.Function;

/**
 * Factory + registry: builds the navigation for a user from their permissions. Adding a screen means
 * adding one registration here; the shell itself does not change (Open/Closed).
 */
public final class PageFactory {
    private record Registration(Permission permission, Function<AppContext, Page> create) {
    }

    private static final List<Registration> REGISTRY = List.of(
            new Registration(Permission.BOOK_FLIGHTS, FlightSearchPage::new),
            new Registration(Permission.VIEW_OWN_RESERVATIONS, MyTripsPage::new),
            new Registration(Permission.MANAGE_ALL_RESERVATIONS, ReservationsPage::new),
            new Registration(Permission.MANAGE_CUSTOMERS, CustomersPage::new),
            new Registration(Permission.MANAGE_FLIGHTS, FlightsPage::new),
            new Registration(Permission.MANAGE_ROUTES_AND_AIRCRAFT, RoutesPage::new),
            new Registration(Permission.MANAGE_ROUTES_AND_AIRCRAFT, AircraftPage::new),
            new Registration(Permission.VIEW_SCHEDULES, SchedulePage::new),
            new Registration(Permission.EDIT_OWN_PROFILE, ProfilePage::new)
    );

    private PageFactory() {
    }

    public static List<Page> pagesFor(User user, AppContext ctx) {
        return REGISTRY.stream()
                .filter(r -> user.can(r.permission()))
                .map(r -> r.create().apply(ctx))
                .toList();
    }
}
