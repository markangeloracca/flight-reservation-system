package ca.ucalgary.frs.domain.model;

/**
 * Price breakdown for one seat: class-adjusted fare, promotional discount, tax and total.
 *
 * @param promotionTitle the promotion that produced the discount, or {@code null}
 */
public record FareQuote(SeatClass seatClass, Money fare, Money discount, Money tax, Money total, String promotionTitle) {

    public boolean hasDiscount() {
        return discount.isPositive();
    }
}
