package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.model.Promotion;
import ca.ucalgary.frs.domain.policy.DiscountPolicy;
import ca.ucalgary.frs.domain.policy.PercentageDiscountPolicy;
import ca.ucalgary.frs.domain.repository.PromotionRepository;
import ca.ucalgary.frs.domain.repository.SettingsRepository;
import ca.ucalgary.frs.domain.repository.TransactionManager;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Use case: Monthly promotional news.
 * The first time the application opens in a month, one active promotion is picked at random and
 * stored for that month. Each customer is shown it once per month, and its discount applies to
 * bookings made during that month.
 */
public final class PromotionController {
    static final String KEY_MONTH = "promotion.month";
    static final String KEY_PROMOTION_ID = "promotion.id";

    private final TransactionManager tx;
    private final PromotionRepository promotions;
    private final SettingsRepository settings;
    private final Session session;
    private final Random random;
    private final Clock clock;

    public PromotionController(TransactionManager tx, PromotionRepository promotions, SettingsRepository settings,
                               Session session, Random random, Clock clock) {
        this.tx = tx;
        this.promotions = promotions;
        this.settings = settings;
        this.session = session;
        this.random = random;
        this.clock = clock;
    }

    /** This month's promotion, selecting one at random if the month has none yet. */
    public Optional<Promotion> currentPromotion() {
        YearMonth month = YearMonth.now(clock);
        return tx.inTransaction(() -> {
            boolean sameMonth = settings.get(KEY_MONTH).map(month.toString()::equals).orElse(false);
            if (sameMonth) {
                return settings.get(KEY_PROMOTION_ID)
                        .filter(s -> !s.isBlank())
                        .flatMap(id -> promotions.findById(Long.parseLong(id)));
            }
            List<Promotion> active = promotions.findActive();
            Optional<Promotion> picked = active.isEmpty()
                    ? Optional.empty()
                    : Optional.of(active.get(random.nextInt(active.size())));
            settings.put(KEY_MONTH, month.toString());
            settings.put(KEY_PROMOTION_ID, picked.map(p -> String.valueOf(p.id())).orElse(""));
            return picked;
        });
    }

    /** The promotion to show the signed-in customer, if they have not seen it this month. */
    public Optional<Promotion> unseenPromotionForCurrentCustomer() {
        Optional<Long> customerId = session.customerId();
        if (customerId.isEmpty()) {
            return Optional.empty();
        }
        YearMonth month = YearMonth.now(clock);
        return currentPromotion().filter(p -> !promotions.hasSeen(customerId.get(), month));
    }

    public void markSeenByCurrentCustomer() {
        session.customerId().ifPresent(id -> promotions.markSeen(id, YearMonth.now(clock)));
    }

    public DiscountPolicy currentDiscountPolicy() {
        return currentPromotion()
                .filter(p -> p.discountPercent() > 0)
                .<DiscountPolicy>map(PercentageDiscountPolicy::new)
                .orElse(DiscountPolicy.NONE);
    }
}
