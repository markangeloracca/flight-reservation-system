package ca.pacific.frs.domain.policy;

import ca.pacific.frs.domain.model.FareQuote;
import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.SeatClass;

/**
 * Prices a seat: class-adjusted fare, minus the discount from a {@link DiscountPolicy}, plus tax.
 */
public final class FareCalculator {
    /** Canadian GST on air travel. */
    public static final int DEFAULT_TAX_PERCENT = 5;

    private final int taxPercent;

    public FareCalculator() {
        this(DEFAULT_TAX_PERCENT);
    }

    public FareCalculator(int taxPercent) {
        this.taxPercent = taxPercent;
    }

    public FareQuote quote(Flight flight, SeatClass seatClass, DiscountPolicy discountPolicy) {
        Money fare = flight.fareFor(seatClass);
        Money discount = Money.min(discountPolicy.discountFor(fare), fare);
        Money taxable = fare.minus(discount);
        Money tax = taxable.percent(taxPercent);
        return new FareQuote(seatClass, fare, discount, tax, taxable.plus(tax),
                discount.isPositive() ? discountPolicy.label() : null);
    }

    public int taxPercent() {
        return taxPercent;
    }
}
