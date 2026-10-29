package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.exception.BusinessRuleException;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Aircraft;
import ca.ucalgary.frs.domain.model.Airport;
import ca.ucalgary.frs.domain.model.Permission;
import ca.ucalgary.frs.domain.model.Route;
import ca.ucalgary.frs.domain.repository.AircraftRepository;
import ca.ucalgary.frs.domain.repository.ReferenceDataRepository;
import ca.ucalgary.frs.domain.repository.ReservationRepository;
import ca.ucalgary.frs.domain.repository.RouteRepository;
import ca.ucalgary.frs.domain.repository.TransactionManager;

import java.util.List;

/** Administrator use cases: Manage routes, Manage aircraft. */
public final class RouteAndAircraftController {
    private final TransactionManager tx;
    private final RouteRepository routes;
    private final AircraftRepository aircraft;
    private final ReferenceDataRepository referenceData;
    private final ReservationRepository reservations;
    private final Session session;
    private final EventBus events;

    public RouteAndAircraftController(TransactionManager tx, RouteRepository routes, AircraftRepository aircraft,
                                      ReferenceDataRepository referenceData, ReservationRepository reservations,
                                      Session session, EventBus events) {
        this.tx = tx;
        this.routes = routes;
        this.aircraft = aircraft;
        this.referenceData = referenceData;
        this.reservations = reservations;
        this.session = session;
        this.events = events;
    }

    public List<Airport> airports() {
        return referenceData.findAllAirports();
    }

    // ---- routes ----

    public List<Route> routes() {
        session.require(Permission.MANAGE_ROUTES_AND_AIRCRAFT);
        return routes.findAll();
    }

    public Route saveRoute(Route route) {
        session.require(Permission.MANAGE_ROUTES_AND_AIRCRAFT);
        Route saved = tx.inTransaction(() -> {
            if (routes.exists(route.origin().code(), route.destination().code(), route.id())) {
                throw new ValidationException("destination", "The route " + route.label() + " already exists.");
            }
            if (route.id() != null) {
                Route existing = routes.findById(route.id()).orElseThrow();
                boolean endpointsChanged = !existing.origin().equals(route.origin())
                        || !existing.destination().equals(route.destination());
                if (endpointsChanged && routes.isInUse(route.id())) {
                    throw new ValidationException("origin",
                            "Flights already use this route, so only its distance can change.");
                }
            }
            return routes.save(route);
        });
        events.publish(EventBus.Topic.ROUTES);
        events.publish(EventBus.Topic.FLIGHTS);
        return saved;
    }

    public void deleteRoute(long routeId) {
        session.require(Permission.MANAGE_ROUTES_AND_AIRCRAFT);
        tx.inTransaction(() -> {
            if (routes.isInUse(routeId)) {
                throw new BusinessRuleException("Flights use this route. Delete or reassign them first.");
            }
            routes.delete(routeId);
        });
        events.publish(EventBus.Topic.ROUTES);
    }

    // ---- aircraft ----

    public List<Aircraft> aircraft() {
        session.require(Permission.MANAGE_ROUTES_AND_AIRCRAFT);
        return aircraft.findAll();
    }

    /** Saves an aircraft. A new seat layout must still contain every seat booked on its flights. */
    public Aircraft saveAircraft(Aircraft a) {
        session.require(Permission.MANAGE_ROUTES_AND_AIRCRAFT);
        Aircraft saved = tx.inTransaction(() -> {
            if (aircraft.registrationExists(a.registration(), a.id())) {
                throw new ValidationException("registration", "Another aircraft already has registration "
                        + a.registration() + ".");
            }
            if (a.id() != null) {
                FlightManagementController.checkSeatsFit(a, reservations.findActiveByAircraft(a.id()));
            }
            return aircraft.save(a);
        });
        events.publish(EventBus.Topic.AIRCRAFT);
        events.publish(EventBus.Topic.FLIGHTS);
        return saved;
    }

    public void deleteAircraft(long aircraftId) {
        session.require(Permission.MANAGE_ROUTES_AND_AIRCRAFT);
        tx.inTransaction(() -> {
            if (aircraft.isInUse(aircraftId)) {
                throw new BusinessRuleException("Flights use this aircraft. Assign them another aircraft first.");
            }
            aircraft.delete(aircraftId);
        });
        events.publish(EventBus.Topic.AIRCRAFT);
    }
}
