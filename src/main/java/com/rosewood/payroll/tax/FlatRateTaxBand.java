package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * A tax band that takes one fixed percentage of the whole gross pay.
 *
 * <p>The whole gross is taxed at the band's rate, not just the part above the band's
 * lower bound. This is how the original code worked and it is kept on purpose: a
 * change to that rule is a business decision, not a refactoring one.
 */
public final class FlatRateTaxBand implements TaxBand {

    private final String name;

    private final Money lowerBound;

    /** Null when the band has no upper limit. Exposed only as an Optional. */
    private final Money upperBound;

    private final BigDecimal rate;

    private FlatRateTaxBand(String name, Money lowerBound, Money upperBound, BigDecimal rate) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(lowerBound, "lowerBound must not be null");
        Objects.requireNonNull(rate, "rate must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        // A rate above 1 would take more tax than the gross pay itself.
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("rate must be between 0 and 1, but was " + rate.toPlainString());
        }

        if (upperBound != null && !lowerBound.isLessThan(upperBound)) {
            throw new IllegalArgumentException(
                    "upper bound " + upperBound + " must be above lower bound " + lowerBound);
        }

        this.name = name;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.rate = rate;
    }

    /**
     * Creates a band that covers gross pay from one amount up to, but not including,
     * another.
     *
     * @param name       the name shown on a payslip
     * @param lowerBound the smallest gross pay covered, inclusive
     * @param upperBound the gross pay at which the next band starts, exclusive
     * @param rate       the share of gross pay taken as tax, from 0 to 1
     * @return the band
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if the name is blank, the rate is outside 0 to 1,
     *                                  or the upper bound is not above the lower bound
     */
    public static FlatRateTaxBand between(String name, Money lowerBound, Money upperBound, BigDecimal rate) {
        Objects.requireNonNull(upperBound, "upperBound must not be null");

        return new FlatRateTaxBand(name, lowerBound, upperBound, rate);
    }

    /**
     * Creates a band that covers all gross pay from one amount upwards, with no upper limit.
     *
     * @param name       the name shown on a payslip
     * @param lowerBound the smallest gross pay covered, inclusive
     * @param rate       the share of gross pay taken as tax, from 0 to 1
     * @return the band
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if the name is blank or the rate is outside 0 to 1
     */
    public static FlatRateTaxBand startingAt(String name, Money lowerBound, BigDecimal rate) {
        return new FlatRateTaxBand(name, lowerBound, null, rate);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Money lowerBound() {
        return lowerBound;
    }

    @Override
    public Optional<Money> upperBound() {
        return Optional.ofNullable(upperBound);
    }

    /**
     * Takes the band's rate of the whole gross pay, rounded to pesewas.
     *
     * @param grossPay gross pay that falls within this band's range
     * @return the tax to take
     */
    @Override
    public Money taxOn(Money grossPay) {
        return grossPay.times(rate);
    }
}
