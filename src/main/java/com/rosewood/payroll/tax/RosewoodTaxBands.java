package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rosewood's current tax bands.
 *
 * <p>This is the one place the bands are listed. The program and the tests both use
 * it, so the tests always check the bands that are really in use. Adding a band means
 * writing its class, if it needs a new rule, and adding it to {@link #schedule()}.
 */
public final class RosewoodTaxBands {

    // These are declared before the bands because static fields are set in the order
    // they are written, and the bands below are built from them.

    // Each limit is shared by two bands: one ends there and the next starts there.
    // Writing it once means the two bands cannot be given different values by a typo.
    private static final Money LOW_TO_MEDIUM = money("1000");

    private static final Money MEDIUM_TO_HIGH = money("5000");

    private static final BigDecimal LOW_RATE = new BigDecimal("0.05");

    private static final BigDecimal MEDIUM_RATE = new BigDecimal("0.15");

    private static final BigDecimal HIGH_RATE = new BigDecimal("0.25");

    /** Gross pay below 1000 is taxed at 5%. */
    public static final TaxBand LOW = FlatRateTaxBand.between(
            "LOW", money("0"), LOW_TO_MEDIUM, LOW_RATE);

    /** Gross pay from 1000 up to, but not including, 5000 is taxed at 15%. */
    public static final TaxBand MEDIUM = FlatRateTaxBand.between(
            "MEDIUM", LOW_TO_MEDIUM, MEDIUM_TO_HIGH, MEDIUM_RATE);

    /** Gross pay of 5000 or more is taxed at 25%. */
    public static final TaxBand HIGH = FlatRateTaxBand.startingAt(
            "HIGH", MEDIUM_TO_HIGH, HIGH_RATE);

    /**
     * Not meant to be created. It only holds the band definitions.
     */
    private RosewoodTaxBands() {
    }

    /**
     * Returns a schedule made of Rosewood's current bands.
     *
     * @return the schedule
     */
    public static TaxSchedule schedule() {
        return new TaxSchedule(List.of(LOW, MEDIUM, HIGH));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
