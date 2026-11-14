package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import java.util.List;
import java.util.Objects;

/**
 * The full set of tax bands, and the one place that decides which band applies to a
 * gross amount.
 */
public class TaxSchedule {

    private final List<TaxBand> bands;

    /**
     * Creates a schedule from its bands.
     *
     * @param bands the bands, in any order
     * @throws NullPointerException if the list, or any band in it, is null
     */
    public TaxSchedule(List<TaxBand> bands) {
        // List.copyOf refuses null bands and stops later changes to the caller's list
        // from changing the schedule.
        this.bands = List.copyOf(Objects.requireNonNull(bands, "bands must not be null"));
    }

    /**
     * Finds the band whose range includes the given gross pay.
     *
     * @param grossPay the gross pay to find a band for
     * @return the band that covers it
     * @throws IllegalStateException if no band covers it, which means the schedule
     *                               was set up wrongly
     */
    public TaxBand bandFor(Money grossPay) {
        return bands.stream()
                .filter(band -> covers(band, grossPay))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no tax band covers gross pay of " + grossPay));
    }

    /**
     * Decides coverage from the range a band declares: from its lower bound, included,
     * up to its upper bound, not included.
     */
    private static boolean covers(TaxBand band, Money grossPay) {
        boolean atOrAboveLowerBound = !grossPay.isLessThan(band.lowerBound());
        boolean belowUpperBound = band.upperBound()
                .map(grossPay::isLessThan)
                .orElse(true);

        return atOrAboveLowerBound && belowUpperBound;
    }
}
