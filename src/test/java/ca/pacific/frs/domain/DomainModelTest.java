package ca.pacific.frs.domain;

import ca.pacific.frs.domain.exception.BusinessRuleException;
import ca.pacific.frs.domain.exception.ValidationException;
import ca.pacific.frs.domain.model.Aircraft;
import ca.pacific.frs.domain.model.Airline;
import ca.pacific.frs.domain.model.Airport;
import ca.pacific.frs.domain.model.CardDetails;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.domain.model.FareQuote;
import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Payment;
import ca.pacific.frs.domain.model.PaymentMethodType;
import ca.pacific.frs.domain.model.PaymentStatus;
import ca.pacific.frs.domain.model.PaymentType;
import ca.pacific.frs.domain.model.Promotion;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.ReservationStatus;
import ca.pacific.frs.domain.model.Route;
import ca.pacific.frs.domain.model.SeatClass;
import ca.pacific.frs.domain.policy.DiscountPolicy;
import ca.pacific.frs.domain.policy.FareCalculator;
import ca.pacific.frs.domain.policy.PercentageDiscountPolicy;
import ca.pacific.frs.domain.policy.StandardRefundPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final Aircraft A320 = new Aircraft(1L, "C-TEST", "Airbus A320", 3, 20, 6);
    private static final Route ROUTE = new Route(1L, new Airport("YYC", "Calgary Intl", "Calgary", "Canada"),
            new Airport("YVR", "Vancouver Intl", "Vancouver", "Canada"), 686);

    private static Flight flight(LocalDateTime departure) {
        return Flight.builder().id(1L).flightNumber("AC100").airline(new Airline("AC", "Air Canada")).route(ROUTE)
                .aircraft(A320).departureTime(departure).arrivalTime(departure.plusMinutes(90))
                .baseFare(Money.of("200.00")).build();
    }

    private static Reservation reservation(LocalDateTime departure, ReservationStatus status) {
        Customer c = Customer.builder().id(1L).firstName("Test").lastName("Person").email("t@example.com").build();
        Flight f = flight(departure);
        FareQuote q = new FareCalculator().quote(f, SeatClass.ECONOMY, DiscountPolicy.NONE);
        return new Reservation(1L, "ABC234", c, f, A320.requireSeat("10C"), q, status, NOW, NOW, null);
    }

    @Test
    void moneyRoundsHalfUpAndFormats() {
        assertEquals(1050, Money.of("10.50").cents());
        assertEquals(Money.of("1.06"), Money.of("21.10").percent(5));
        assertEquals("$1,234.50", Money.of("1234.5").toString());
    }

    @Test
    void aircraftLayoutPutsBusinessRowsFirst() {
        assertEquals(138, A320.capacity());
        assertEquals(SeatClass.BUSINESS, A320.requireSeat("3F").seatClass());
        assertEquals(SeatClass.ECONOMY, A320.requireSeat("4A").seatClass());
        assertTrue(A320.seat("24A").isEmpty());
        assertTrue(A320.seat("5G").isEmpty());
    }

    @Test
    void flightRejectsArrivalBeforeDepartureAndWrongAirlinePrefix() {
        assertThrows(ValidationException.class, () -> flight(NOW).toBuilder().arrivalTime(NOW.minusHours(1)).build());
        assertThrows(ValidationException.class, () -> flight(NOW).toBuilder().flightNumber("WS100").build());
    }

    @Test
    void fareIncludesClassMultiplierDiscountAndTax() {
        Flight f = flight(NOW.plusDays(3));
        Promotion promo = new Promotion(1L, "Test", "desc", 10, true);
        FareQuote q = new FareCalculator().quote(f, SeatClass.BUSINESS, new PercentageDiscountPolicy(promo));
        assertEquals(Money.of("500.00"), q.fare());
        assertEquals(Money.of("50.00"), q.discount());
        assertEquals(Money.of("22.50"), q.tax());
        assertEquals(Money.of("472.50"), q.total());
    }

    @Test
    void reservationStateMachine() {
        Reservation r = reservation(NOW.plusDays(5), ReservationStatus.PENDING_PAYMENT);
        assertThrows(BusinessRuleException.class, () -> r.complete(NOW));
        r.confirm(NOW);
        assertEquals(ReservationStatus.CONFIRMED, r.status());
        assertThrows(BusinessRuleException.class, () -> r.confirm(NOW));
        r.cancel(NOW);
        assertEquals(ReservationStatus.CANCELLED, r.status());
        assertFalse(r.holdsSeat());
        assertThrows(BusinessRuleException.class, () -> r.cancel(NOW));
    }

    @Test
    void departedReservationCannotBeCancelled() {
        Reservation r = reservation(NOW.minusHours(1), ReservationStatus.CONFIRMED);
        assertThrows(BusinessRuleException.class, () -> r.cancel(NOW));
    }

    @Test
    void refundPolicyIsFullEarlyAndHalfLate() {
        StandardRefundPolicy policy = new StandardRefundPolicy();
        Money paid = Money.of("210.00");
        assertEquals(paid, policy.refundFor(reservation(NOW.plusDays(2), ReservationStatus.CONFIRMED), paid, NOW));
        assertEquals(Money.of("105.00"), policy.refundFor(reservation(NOW.plusHours(5), ReservationStatus.CONFIRMED), paid, NOW));
    }

    @Test
    void cardValidationUsesLuhnAndExpiry() {
        assertTrue(CardDetails.luhnValid("4242424242424242"));
        assertFalse(CardDetails.luhnValid("4242424242424241"));
        assertThrows(ValidationException.class, () -> new CardDetails(PaymentMethodType.CREDIT_CARD, "A B",
                "4242424242424241", YearMonth.now().plusYears(1), "123"));
        assertThrows(ValidationException.class, () -> new CardDetails(PaymentMethodType.CREDIT_CARD, "A B",
                "4242424242424242", YearMonth.now().minusMonths(1), "123"));
        CardDetails ok = new CardDetails(PaymentMethodType.CREDIT_CARD, "A B", "4242 4242 4242 4242",
                YearMonth.now().plusYears(1), "123");
        assertEquals("Visa", ok.brand());
        assertFalse(ok.toString().contains("4242424242424242"));
    }

    @Test
    void paymentStatusTransitionsAreEnforced() {
        Payment p = Payment.initiate(1, PaymentType.CHARGE, Money.of("10.00"), TestCards.visa(), NOW);
        assertThrows(IllegalStateException.class, p::complete);
        p.authorize("AUTH-1");
        p.complete();
        assertEquals(PaymentStatus.COMPLETED, p.status());
        assertEquals(Money.of("10.00"), p.netEffect());
        assertThrows(IllegalStateException.class, () -> p.decline("late"));
    }

    @Test
    void customerValidatesEmail() {
        ValidationException e = assertThrows(ValidationException.class, () ->
                Customer.builder().firstName("A").lastName("B").email("not-an-email").build());
        assertEquals("email", e.field());
    }

    static final class TestCards {
        static CardDetails visa() {
            return new CardDetails(PaymentMethodType.CREDIT_CARD, "A B", "4242424242424242", YearMonth.now().plusYears(1), "123");
        }
    }
}
