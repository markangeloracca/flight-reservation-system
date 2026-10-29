package ca.ucalgary.frs.control;

import ca.ucalgary.frs.TestContext;
import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.domain.exception.AuthorizationException;
import ca.ucalgary.frs.domain.exception.BusinessRuleException;
import ca.ucalgary.frs.domain.exception.PaymentDeclinedException;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.domain.model.Payment;
import ca.ucalgary.frs.domain.model.PaymentStatus;
import ca.ucalgary.frs.domain.model.PaymentType;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.ReservationStatus;
import ca.ucalgary.frs.control.confirmation.PlainTextConfirmationRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingFlowTest {
    @TempDir
    Path dir;
    TestContext t;
    AppContext ctx;

    @BeforeEach
    void setUp() {
        t = new TestContext(dir).login("customer", "customer123");
        ctx = t.ctx;
    }

    private long me() {
        return ctx.session().customerId().orElseThrow();
    }

    private FlightAvailability someFlight(int daysAhead) {
        return ctx.flightSearch().search("YYC", "YVR", TestContext.START.toLocalDate().plusDays(daysAhead), TestContext.START.toLocalDate().plusDays(daysAhead), null).get(0);
    }

    private Reservation book(FlightAvailability f, String seat) {
        Reservation hold = ctx.booking().holdSeat(me(), f.flight().id(), seat);
        return ctx.booking().pay(hold.id(), TestContext.goodCard());
    }

    @Test
    void searchFiltersByRouteDateAndAirline() {
        List<FlightAvailability> all = ctx.flightSearch().search("YYC", "YVR", TestContext.START.toLocalDate().plusDays(3), TestContext.START.toLocalDate().plusDays(3), null);
        assertFalse(all.isEmpty());
        assertTrue(all.stream().allMatch(a -> a.flight().route().label().equals("YYC → YVR")));
        String airline = all.get(0).flight().airline().code();
        List<FlightAvailability> one = ctx.flightSearch().search("YYC", "YVR",
                TestContext.START.toLocalDate().plusDays(3), TestContext.START.toLocalDate().plusDays(3), airline);
        assertTrue(one.stream().allMatch(a -> a.flight().airline().code().equals(airline)));
    }

    @Test
    void searchCoversEveryDayInTheRange() {
        LocalDate from = TestContext.START.toLocalDate().plusDays(3);
        LocalDate to = from.plusDays(2);
        List<FlightAvailability> range = ctx.flightSearch().search("YYC", "YVR", from, to, null);
        assertTrue(range.stream().map(a -> a.flight().departureTime().toLocalDate())
                .allMatch(d -> !d.isBefore(from) && !d.isAfter(to)));
        List<Long> ids = range.stream().map(a -> a.flight().id()).toList();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<FlightAvailability> day = ctx.flightSearch().search("YYC", "YVR", d, d, null);
            assertFalse(day.isEmpty());
            assertTrue(day.stream().allMatch(a -> ids.contains(a.flight().id())));
        }
    }

    @Test
    void holdPayConfirmProducesConfirmation() throws Exception {
        FlightAvailability f = someFlight(5);
        Reservation r = book(f, "10A");
        assertEquals(ReservationStatus.CONFIRMED, r.status());
        assertTrue(ctx.flightSearch().seatMap(f.flight().id()).occupiedSeatCodes().contains("10A"));
        assertEquals(r.fare().total(), ctx.payment().amountPaid(r.id()));

        Path file = dir.resolve("confirmation.txt");
        ctx.booking().exportConfirmation(r.id(), file, new PlainTextConfirmationRenderer());
        String text = Files.readString(file);
        assertTrue(text.contains(r.confirmationCode()));
        assertFalse(text.contains("4242 4242 4242 4242"));
    }

    @Test
    void declinedPaymentIsRecordedAndHoldStaysForRetry() {
        FlightAvailability f = someFlight(5);
        Reservation hold = ctx.booking().holdSeat(me(), f.flight().id(), "11B");
        assertThrows(PaymentDeclinedException.class, () -> ctx.booking().pay(hold.id(), TestContext.declinedCard()));
        assertEquals(ReservationStatus.PENDING_PAYMENT, ctx.booking().get(hold.id()).status());
        List<Payment> attempts = ctx.payment().paymentsFor(hold.id());
        assertEquals(PaymentStatus.DECLINED, attempts.get(0).status());

        Reservation paid = ctx.booking().pay(hold.id(), TestContext.goodCard());
        assertEquals(ReservationStatus.CONFIRMED, paid.status());
    }

    @Test
    void seatCannotBeDoubleBooked() {
        FlightAvailability f = someFlight(6);
        ctx.booking().holdSeat(me(), f.flight().id(), "12C");
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> ctx.booking().holdSeat(me(), f.flight().id(), "12C"));
        assertTrue(e.getMessage().contains("12C"));
    }

    @Test
    void releasedHoldFreesTheSeat() {
        FlightAvailability f = someFlight(6);
        Reservation hold = ctx.booking().holdSeat(me(), f.flight().id(), "14D");
        ctx.booking().releaseHold(hold.id());
        assertFalse(ctx.flightSearch().seatMap(f.flight().id()).occupiedSeatCodes().contains("14D"));
        ctx.booking().holdSeat(me(), f.flight().id(), "14D");
    }

    @Test
    void earlyCancellationRefundsInFull() {
        Reservation r = book(someFlight(7), "9A");
        var result = ctx.booking().cancel(r.id());
        assertEquals(r.fare().total(), result.refund());
        assertEquals(ReservationStatus.CANCELLED, ctx.booking().get(r.id()).status());
        assertEquals(Money.ZERO, ctx.payment().amountPaid(r.id()));
        assertTrue(ctx.payment().paymentsFor(r.id()).stream()
                .anyMatch(p -> p.type() == PaymentType.REFUND && p.status() == PaymentStatus.COMPLETED));
    }

    @Test
    void lateCancellationRefundsHalf() {
        FlightAvailability f = someFlight(2);
        Reservation r = book(f, "9B");
        t.clock.set(f.flight().departureTime().minusHours(3));
        var result = ctx.booking().cancel(r.id());
        assertEquals(r.fare().total().percent(50), result.refund());
    }

    @Test
    void modifyToPricierSeatChargesDifferenceAndCheaperRefunds() {
        FlightAvailability f = someFlight(8);
        Reservation r = book(f, "10A");
        Money paid = ctx.payment().amountPaid(r.id());

        Reservation business = ctx.booking().modify(r.id(), f.flight().id(), "1A", TestContext.goodCard());
        assertEquals("1A", business.seat().code());
        assertEquals(business.fare().total(), ctx.payment().amountPaid(r.id()));
        assertTrue(business.fare().total().compareTo(paid) > 0);

        Reservation back = ctx.booking().modify(r.id(), f.flight().id(), "10B", null);
        assertEquals(back.fare().total(), ctx.payment().amountPaid(r.id()));
        assertFalse(ctx.flightSearch().seatMap(f.flight().id()).occupiedSeatCodes().contains("1A"));
    }

    @Test
    void declinedFareDifferenceLeavesReservationUnchanged() {
        FlightAvailability f = someFlight(8);
        Reservation r = book(f, "10A");
        assertThrows(PaymentDeclinedException.class,
                () -> ctx.booking().modify(r.id(), f.flight().id(), "1A", TestContext.declinedCard()));
        assertEquals("10A", ctx.booking().get(r.id()).seat().code());
    }

    @Test
    void customerCannotSeeAnotherCustomersReservation() {
        Reservation mine = book(someFlight(9), "15A");
        ctx.auth().logout();
        Customer other = Customer.builder().firstName("Other").lastName("Person").email("other@example.com").build();
        ctx.auth().registerCustomer(other, "other", "password1".toCharArray(), "password1".toCharArray());
        assertThrows(AuthorizationException.class, () -> ctx.booking().get(mine.id()));
        assertThrows(AuthorizationException.class, () -> ctx.booking().cancel(mine.id()));
    }

    @Test
    void housekeepingExpiresHoldsAndCompletesFlownTrips() {
        FlightAvailability f = someFlight(1);
        Reservation hold = ctx.booking().holdSeat(me(), f.flight().id(), "16A");
        Reservation paid = book(f, "16B");
        t.clock.set(f.flight().departureTime().plusHours(1));
        var result = ctx.booking().runHousekeeping();
        assertTrue(result.expiredHolds() >= 1);
        assertTrue(result.completedTrips() >= 1);
        assertEquals(ReservationStatus.CANCELLED, ctx.booking().get(hold.id()).status());
        assertEquals(ReservationStatus.COMPLETED, ctx.booking().get(paid.id()).status());
    }

    @Test
    void holdExpiresAfterFifteenMinutes() {
        Reservation hold = ctx.booking().holdSeat(me(), someFlight(4).flight().id(), "17A");
        t.clock.advance(Duration.ofMinutes(10));
        ctx.booking().runHousekeeping();
        assertEquals(ReservationStatus.PENDING_PAYMENT, ctx.booking().get(hold.id()).status());
        t.clock.advance(Duration.ofMinutes(6));
        ctx.booking().runHousekeeping();
        assertEquals(ReservationStatus.CANCELLED, ctx.booking().get(hold.id()).status());
    }
}
