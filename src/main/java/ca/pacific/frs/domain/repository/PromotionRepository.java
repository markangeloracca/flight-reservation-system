package ca.pacific.frs.domain.repository;

import ca.pacific.frs.domain.model.Promotion;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository {
    List<Promotion> findActive();

    Optional<Promotion> findById(long id);

    boolean hasSeen(long customerId, YearMonth month);

    void markSeen(long customerId, YearMonth month);
}
