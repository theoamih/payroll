package com.rosewood.payroll.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of money in Ghana cedis, always held as a whole number of pesewas.
 *
 * <p>Every amount is rounded to two decimal places when it is created, and this is the
 * only place where that rounding happens, so the rule cannot drift between one part of
 * a payslip and another.
 *
 * <p>Amounts are held as {@link BigDecimal} rather than {@code double}, because binary
 * floating point cannot represent most decimal amounts exactly and produced results
 * such as {@code 3.9425000000000003} in the original code.
 *
 * @param amount the amount in cedis, rounded to pesewas
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int PESEWA_DECIMAL_PLACES = 2;

    /**
     * Halves round up (0.005 becomes 0.01) because that is how a person checks a
     * payslip by hand, so an employee working it out gets the same answer.
     */
    private static final RoundingMode PAYSLIP_ROUNDING = RoundingMode.HALF_UP;

    /**
     * Rounds the amount to whole pesewas.
     *
     * @param amount the amount in cedis, at any precision
     * @throws NullPointerException if amount is null
     */
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");

        amount = amount.setScale(PESEWA_DECIMAL_PLACES, PAYSLIP_ROUNDING);
    }

    /**
     * Multiplies this amount, for example by a tax rate, and rounds the result to pesewas.
     *
     * @param factor the value to multiply by
     * @return the product, rounded to pesewas
     */
    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    /**
     * Takes another amount away from this one. No rounding is needed, because both
     * amounts are already whole pesewas.
     *
     * @param other the amount to take away
     * @return this amount minus the other
     */
    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /**
     * Compares this amount with another by value.
     *
     * @param other the amount to compare with
     * @return true if this amount is smaller than the other
     */
    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    /**
     * Orders amounts by value, so amounts can be sorted.
     *
     * <p>Because every amount is held to two decimal places, two amounts compare as
     * equal exactly when {@link #equals(Object)} says they are equal.
     *
     * @param other the amount to compare with
     * @return a negative number, zero, or a positive number as this amount is smaller
     *         than, equal to, or larger than the other
     */
    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    /**
     * Formats the amount as it would appear on a payslip.
     *
     * @return the amount with its currency, for example "GHS 760.00"
     */
    @Override
    public String toString() {
        return "GHS " + amount.toPlainString();
    }
}
