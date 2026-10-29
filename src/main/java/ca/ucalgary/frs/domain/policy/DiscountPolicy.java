package ca.ucalgary.frs.domain.policy;

import ca.ucalgary.frs.domain.model.Money;

/** Strategy: how much to take off a fare. */
public interface DiscountPolicy {

    Money discountFor(Money fare);

    /** Short label shown on the fare breakdown, or {@code null} when there is no discount. */
    String label();

    DiscountPolicy NONE = new DiscountPolicy() {
        @Override
        public Money discountFor(Money fare) {
            return Money.ZERO;
        }

        @Override
        public String label() {
            return null;
        }
    };
}
