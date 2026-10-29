package ca.pacific.frs.control;

import ca.pacific.frs.domain.model.SeatClass;
import ca.pacific.frs.TestContext;
import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.domain.exception.AuthorizationException;
import ca.pacific.frs.domain.exception.AuthenticationException;
import ca.pacific.frs.domain.exception.BusinessRuleException;
import ca.pacific.frs.domain.exception.ValidationException;
import ca.pacific.frs.domain.model.Aircraft;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.FlightAvailability;
import ca.pacific.frs.domain.model.FlightStatus;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Promotion;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.ReservationStatus;
import ca.pacific.frs.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministrationTest {
    @TempDir
    Path dir;

    @Test
    void loginChecksPasswordAndRole() {
        AppContext ctx = new TestContext(dir).ctx;
        assertThrows(AuthenticationException.class, () -> ctx.auth().login("admin", "wrong".toCharArray()));
        assertThrows(AuthenticationException.class, () -> ctx.auth().login("nobody", "admin123".toCharArray()));
        assertEquals(Role.ADMIN, ctx.auth().login("admin", "admin123".toCharArray()).role());
        assertThrows(AuthorizationException.class, () -> ctx.customers().search(""));
    }

    @Test
    void registrationRejectsWeakPasswordAndDuplicateUsername() {
        AppContext ctx = new TestContext(dir).ctx;
        Customer c = Customer.builder().firstName("New").lastName("Person").email("new@example.com").build();
        ValidationException weak = assertThrows(ValidationException.class,
                () -> ctx.auth().registerCustomer(c, "newbie", "short".toCharArray(), "short".toCharArray()));
        assertEquals("password", weak.field());
        ValidationException taken = assertThrows(ValidationException.class,
                () -> ctx.auth().registerCustomer(c, "agent", "password1".toCharArray(), "password1".toCharArray()));
        assertEquals("username", taken.field());
        assertEquals(Role.CUSTOMER, ctx.auth().registerCustomer(c, "newbie", "password1".toCharArray(),
                "password1".toCharArray()).role());
    }

    @Test
    void agentManagesCustomersAndBooksForThem() {
        TestContext t = new TestContext(dir).login("agent", "agent123");
        AppContext ctx = t.ctx;
        Customer created = ctx.customers().create(Customer.builder().firstName("Walk").lastName("In")
                .email("walk.in@example.com").build());
        assertThrows(ValidationException.class, () -> ctx.customers().create(Customer.builder().firstName("Dup")
                .lastName("Email").email("WALK.IN@example.com").build()));
        FlightAvailability f = ctx.flightSearch().search("YYZ", "YUL", TestContext.START.toLocalDate().plusDays(3), TestContext.START.toLocalDate().plusDays(3), null).get(0);
        Reservation hold = ctx.booking().holdSeat(created.id(), f.flight().id(), "8C");
        ctx.booking().pay(hold.id(), TestContext.goodCard());
        assertEquals(1, ctx.booking().search(hold.confirmationCode(), null).size());
        assertEquals(1, ctx.booking().search("Walk In", ReservationStatus.CONFIRMED).size());
    }

    @Test
    void cancellingAFlightRefundsEveryPassenger() {
        TestContext t = new TestContext(dir).login("customer", "customer123");
        AppContext ctx = t.ctx;
        FlightAvailability f = ctx.flightSearch().search("YYC", "YEG", TestContext.START.toLocalDate().plusDays(4), TestContext.START.toLocalDate().plusDays(4), null).get(0);
        Reservation r = ctx.booking().holdSeat(ctx.session().customerId().orElseThrow(), f.flight().id(), "5A");
        ctx.booking().pay(r.id(), TestContext.goodCard());
        ctx.auth().logout();
        ctx.auth().login("admin", "admin123".toCharArray());

        assertThrows(BusinessRuleException.class, () -> ctx.flightManagement().deleteFlight(f.flight().id()));
        assertEquals(1, ctx.flightManagement().cancelFlight(f.flight().id()));
        assertEquals(Money.ZERO, ctx.payment().amountPaid(r.id()));
        assertEquals(FlightStatus.CANCELLED, ctx.flightSearch().flight(f.flight().id()).status());
    }

    @Test
    void flightEditsMustKeepBookedSeatsValid() {
        TestContext t = new TestContext(dir).login("customer", "customer123");
        AppContext ctx = t.ctx;
        FlightAvailability f = ctx.flightSearch().search("YYC", "YYZ", TestContext.START.toLocalDate().plusDays(4), TestContext.START.toLocalDate().plusDays(4), null).get(0);
        Reservation r = ctx.booking().holdSeat(ctx.session().customerId().orElseThrow(), f.flight().id(), "1A");
        ctx.booking().pay(r.id(), TestContext.goodCard());
        ctx.auth().logout();
        ctx.auth().login("admin", "admin123".toCharArray());

        Aircraft noBusiness = ctx.flightManagement().aircraft().stream().filter(a -> a.businessRows() == 0)
                .findFirst().orElseThrow();
        Flight moved = f.flight().toBuilder().aircraft(noBusiness).build();
        ValidationException e = assertThrows(ValidationException.class, () -> ctx.flightManagement().save(moved));
        assertEquals("aircraft", e.field());

        Flight delayed = f.flight().toBuilder().status(FlightStatus.DELAYED)
                .departureTime(f.flight().departureTime().plusHours(1))
                .arrivalTime(f.flight().arrivalTime().plusHours(1)).build();
        assertEquals(FlightStatus.DELAYED, ctx.flightManagement().save(delayed).status());
    }

    @Test
    void removeOutdatedKeepsFlightsWithHistory() {
        TestContext t = new TestContext(dir).login("admin", "admin123");
        AppContext ctx = t.ctx;
        LocalDate start = TestContext.START.toLocalDate();
        int removed = ctx.flightManagement().removeOutdatedFlights();
        assertTrue(removed > 0);
        // the demo customer's past trip is history and must survive
        assertTrue(ctx.flightManagement().flights(start.minusDays(7), start).stream()
                .anyMatch(a -> a.totalAvailable() < a.capacity()));
    }

    @Test
    void routesAndAircraftInUseCannotBeDeleted() {
        AppContext ctx = new TestContext(dir).login("admin", "admin123").ctx;
        long routeInUse = ctx.routesAndAircraft().routes().get(0).id();
        assertThrows(BusinessRuleException.class, () -> ctx.routesAndAircraft().deleteRoute(routeInUse));
        Aircraft a = ctx.routesAndAircraft().saveAircraft(new Aircraft(null, "C-NEW1", "Test Jet", 2, 10, 4));
        assertThrows(ValidationException.class, () ->
                ctx.routesAndAircraft().saveAircraft(new Aircraft(null, "C-NEW1", "Dup", 1, 10, 4)));
        ctx.routesAndAircraft().deleteAircraft(a.id());
    }

    @Test
    void monthlyPromotionIsStableWithinAMonthAndShownOnce() {
        TestContext t = new TestContext(dir).login("customer", "customer123");
        AppContext ctx = t.ctx;
        Optional<Promotion> first = ctx.promotions().currentPromotion();
        assertTrue(first.isPresent());
        assertEquals(first, ctx.promotions().currentPromotion());

        assertTrue(ctx.promotions().unseenPromotionForCurrentCustomer().isPresent());
        ctx.promotions().markSeenByCurrentCustomer();
        assertFalse(ctx.promotions().unseenPromotionForCurrentCustomer().isPresent());

        t.clock.set(TestContext.START.plusMonths(1).withDayOfMonth(1));
        assertTrue(ctx.promotions().unseenPromotionForCurrentCustomer().isPresent());

        LocalDate day = t.clock.instant().atZone(t.clock.getZone()).toLocalDate().plusDays(3);
        FlightAvailability f = ctx.flightSearch().search("YYC", "YVR", day, day, null).get(0);
        assertTrue(ctx.booking().quote(f.flight(), SeatClass.ECONOMY).hasDiscount());
    }
}
