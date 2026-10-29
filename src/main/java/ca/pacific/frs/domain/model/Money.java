package ca.pacific.frs.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Immutable amount of Canadian dollars, stored as whole cents to avoid floating-point rounding errors.
 */
public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);
    private static final Locale LOCALE = Locale.CANADA;

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /** Parses a dollar amount such as {@code "249.99"}. */
    public static Money of(String dollars) {
        try {
            BigDecimal value = new BigDecimal(dollars.trim().replace("$", "").replace(",", ""));
            return new Money(value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact());
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Not a valid amount: " + dollars, e);
        }
    }

    public static Money ofDollars(double dollars) {
        return of(BigDecimal.valueOf(dollars).toPlainString());
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(cents, other.cents));
    }

    /** Multiplies by a factor, rounding half-up to the nearest cent. */
    public Money times(BigDecimal factor) {
        return new Money(BigDecimal.valueOf(cents).multiply(factor).setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    /** Returns {@code percent}% of this amount, rounded half-up. */
    public Money percent(int percent) {
        return times(BigDecimal.valueOf(percent).movePointLeft(2));
    }

    public Money negate() {
        return new Money(-cents);
    }

    public Money abs() {
        return cents < 0 ? negate() : this;
    }

    public boolean isZero() {
        return cents == 0;
    }

    public boolean isPositive() {
        return cents > 0;
    }

    public boolean isNegative() {
        return cents < 0;
    }

    public static Money max(Money a, Money b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Money min(Money a, Money b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(cents, 2);
    }

    /** Plain decimal text without currency symbol, e.g. {@code 249.99}. */
    public String toPlainString() {
        return toDecimal().toPlainString();
    }

    @Override
    public int compareTo(Money o) {
        return Long.compare(cents, o.cents);
    }

    @Override
    public String toString() {
        return NumberFormat.getCurrencyInstance(LOCALE).format(toDecimal());
    }
}
