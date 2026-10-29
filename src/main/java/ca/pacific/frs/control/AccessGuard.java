package ca.pacific.frs.control;

import ca.pacific.frs.domain.exception.AuthorizationException;
import ca.pacific.frs.domain.model.Permission;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.User;

/** Shared ownership rules: agents may act on any customer; customers only on themselves. */
final class AccessGuard {
    private final Session session;

    AccessGuard(Session session) {
        this.session = session;
    }

    User requireCustomerAccess(long customerId) {
        User user = session.requireUser();
        if (user.can(Permission.MANAGE_CUSTOMERS) || user.can(Permission.MANAGE_ALL_RESERVATIONS)) {
            return user;
        }
        if (session.customerId().filter(id -> id == customerId).isPresent()) {
            return user;
        }
        throw new AuthorizationException("You can only access your own account.");
    }

    User requireReservationAccess(Reservation reservation) {
        return requireCustomerAccess(reservation.customer().id());
    }
}
