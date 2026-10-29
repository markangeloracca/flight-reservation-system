package ca.pacific.frs.app;

import ca.pacific.frs.control.AuthController;
import ca.pacific.frs.control.BookingController;
import ca.pacific.frs.control.CustomerController;
import ca.pacific.frs.control.EventBus;
import ca.pacific.frs.control.FlightManagementController;
import ca.pacific.frs.control.FlightSearchController;
import ca.pacific.frs.control.PaymentController;
import ca.pacific.frs.control.PromotionController;
import ca.pacific.frs.control.RouteAndAircraftController;
import ca.pacific.frs.control.Session;
import ca.pacific.frs.data.db.ConnectionFactory;
import ca.pacific.frs.data.db.Database;
import ca.pacific.frs.data.file.LocalFileExporter;
import ca.pacific.frs.data.gateway.SimulatedPaymentGateway;
import ca.pacific.frs.data.migration.DemoDataSeeder;
import ca.pacific.frs.data.migration.SchemaMigrator;
import ca.pacific.frs.data.migration.SqlScriptMigration;
import ca.pacific.frs.data.sqlite.SqliteAircraftRepository;
import ca.pacific.frs.data.sqlite.SqliteCustomerRepository;
import ca.pacific.frs.data.sqlite.SqliteFlightRepository;
import ca.pacific.frs.data.sqlite.SqlitePaymentRepository;
import ca.pacific.frs.data.sqlite.SqlitePromotionRepository;
import ca.pacific.frs.data.sqlite.SqliteReferenceDataRepository;
import ca.pacific.frs.data.sqlite.SqliteReservationRepository;
import ca.pacific.frs.data.sqlite.SqliteRouteRepository;
import ca.pacific.frs.data.sqlite.SqliteSettingsRepository;
import ca.pacific.frs.data.sqlite.SqliteUserRepository;
import ca.pacific.frs.domain.gateway.PaymentGateway;
import ca.pacific.frs.domain.policy.ConfirmationCodeGenerator;
import ca.pacific.frs.domain.policy.FareCalculator;
import ca.pacific.frs.domain.policy.StandardRefundPolicy;
import ca.pacific.frs.domain.repository.AircraftRepository;
import ca.pacific.frs.domain.repository.CustomerRepository;
import ca.pacific.frs.domain.repository.FlightRepository;
import ca.pacific.frs.domain.repository.PaymentRepository;
import ca.pacific.frs.domain.repository.ReferenceDataRepository;
import ca.pacific.frs.domain.repository.ReservationRepository;
import ca.pacific.frs.domain.repository.RouteRepository;
import ca.pacific.frs.domain.repository.UserRepository;
import ca.pacific.frs.domain.security.PasswordHasher;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

/**
 * Composition root: builds every object once and wires dependencies through constructors.
 * Nothing else in the application calls {@code new} on a repository or controller.
 */
public final class AppContext {
    private final Database database;
    private final Session session = new Session();
    private final EventBus events = new EventBus();

    private final AuthController auth;
    private final FlightSearchController flightSearch;
    private final BookingController booking;
    private final PaymentController payment;
    private final CustomerController customers;
    private final FlightManagementController flightManagement;
    private final RouteAndAircraftController routesAndAircraft;
    private final PromotionController promotions;

    public AppContext(ConnectionFactory connections, Clock clock, PasswordHasher hasher, PaymentGateway gateway) {
        this.database = new Database(connections);
        new SchemaMigrator(database, List.of(
                new SqlScriptMigration(1, "/db/V1__schema.sql"),
                new DemoDataSeeder(hasher, clock)
        )).migrate();

        UserRepository userRepo = new SqliteUserRepository(database);
        CustomerRepository customerRepo = new SqliteCustomerRepository(database);
        ReferenceDataRepository referenceRepo = new SqliteReferenceDataRepository(database);
        RouteRepository routeRepo = new SqliteRouteRepository(database);
        AircraftRepository aircraftRepo = new SqliteAircraftRepository(database);
        FlightRepository flightRepo = new SqliteFlightRepository(database);
        ReservationRepository reservationRepo = new SqliteReservationRepository(database);
        PaymentRepository paymentRepo = new SqlitePaymentRepository(database);

        this.auth = new AuthController(database, userRepo, customerRepo, hasher, session, events);
        this.flightSearch = new FlightSearchController(flightRepo, referenceRepo, session, clock);
        this.promotions = new PromotionController(database, new SqlitePromotionRepository(database),
                new SqliteSettingsRepository(database), session, new SecureRandom(), clock);
        this.payment = new PaymentController(database, paymentRepo, reservationRepo, gateway, session, events, clock);
        this.booking = new BookingController(database, reservationRepo, flightRepo, customerRepo, paymentRepo, payment,
                promotions, new FareCalculator(), new StandardRefundPolicy(), new ConfirmationCodeGenerator(),
                new LocalFileExporter(), session, events, clock);
        this.customers = new CustomerController(database, customerRepo, userRepo, reservationRepo, paymentRepo,
                session, events);
        this.flightManagement = new FlightManagementController(database, flightRepo, routeRepo, aircraftRepo,
                referenceRepo, reservationRepo, payment, session, events, clock);
        this.routesAndAircraft = new RouteAndAircraftController(database, routeRepo, aircraftRepo, referenceRepo,
                reservationRepo, session, events);
    }

    /** Production wiring: system clock, full-strength hashing, gateway with visible latency. */
    public static AppContext production(ConnectionFactory connections) {
        return new AppContext(connections, Clock.systemDefaultZone(), new PasswordHasher(),
                new SimulatedPaymentGateway(700));
    }

    public Database database() {
        return database;
    }

    public Session session() {
        return session;
    }

    public EventBus events() {
        return events;
    }

    public AuthController auth() {
        return auth;
    }

    public FlightSearchController flightSearch() {
        return flightSearch;
    }

    public BookingController booking() {
        return booking;
    }

    public PaymentController payment() {
        return payment;
    }

    public CustomerController customers() {
        return customers;
    }

    public FlightManagementController flightManagement() {
        return flightManagement;
    }

    public RouteAndAircraftController routesAndAircraft() {
        return routesAndAircraft;
    }

    public PromotionController promotions() {
        return promotions;
    }
}
