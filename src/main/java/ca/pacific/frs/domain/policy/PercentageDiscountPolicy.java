package ca.pacific.frs.domain.policy;

import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Promotion;

/** Takes a fixed percentage off the fare, as defined by a monthly {@link Promotion}. */
public final class PercentageDiscountPolicy implements DiscountPolicy {
    private final Promotion promotion;

    public PercentageDiscountPolicy(Promotion promotion) {
        this.promotion = promotion;
    }

    @Override
    public Money discountFor(Money fare) {
        return fare.percent(promotion.discountPercent());
    }

    @Override
    public String label() {
        return promotion.title() + " (" + promotion.discountPercent() + "% off)";
    }
}
