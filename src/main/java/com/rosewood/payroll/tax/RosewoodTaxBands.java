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

    /** Gross pay below 1000 is taxed at 5%. */
    public static final TaxBand LOW = FlatRateTaxBand.between(
            "LOW", money("0"), money("1000"), new BigDecimal("0.05"));

    /** Gross pay from 1000 up to, but not including, 5000 is taxed at 15%. */
    public static final TaxBand MEDIUM = FlatRateTaxBand.between(
            "MEDIUM", money("1000"), money("5000"), new BigDecimal("0.15"));

    /** Gross pay of 5000 or more is taxed at 25%. */
    public static final TaxBand HIGH = FlatRateTaxBand.startingAt(
            "HIGH", money("5000"), new BigDecimal("0.25"));

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
