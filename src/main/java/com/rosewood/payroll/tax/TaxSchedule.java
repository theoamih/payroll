package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The full set of tax bands, and the one place that decides which band applies to a
 * gross amount.
 *
 * <p>The bands are checked when the schedule is created: together they must cover
 * every gross amount from zero upwards, with no gaps and no overlaps. A schedule that
 * exists therefore has exactly one band for any gross pay of zero or more.
 */
public class TaxSchedule {

    private static final Money ZERO = new Money(BigDecimal.ZERO);

    /** Ordered from the lowest band to the highest. */
    private final List<TaxBand> bands;

    /**
     * Creates a schedule from its bands, after checking that they fit together.
     *
     * @param bands the bands, in any order
     * @throws NullPointerException         if the list, or any band in it, is null
     * @throws InvalidTaxScheduleException if the bands do not start at zero, leave a gap,
     *                                      overlap, or do not end with one band that has
     *                                      no upper limit
     */
    public TaxSchedule(List<TaxBand> bands) {
        // List.copyOf refuses null bands, and working from a copy stops later changes to
        // the caller's list from changing the schedule.
        List<TaxBand> ordered = List.copyOf(Objects.requireNonNull(bands, "bands must not be null"))
                .stream()
                .sorted(Comparator.comparing(TaxBand::lowerBound))
                .toList();

        checkTheBandsCoverAllGrossPayFromZero(ordered);

        this.bands = ordered;
    }

    /**
     * Finds the band whose range includes the given gross pay.
     *
     * @param grossPay the gross pay to find a band for
     * @return the band that covers it
     * @throws IllegalStateException if no band covers it, which can only happen for a
     *                               negative amount, since the bands cover everything
     *                               from zero upwards
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

    /**
     * Checks that the bands, lowest first, run from zero with each band ending exactly
     * where the next begins, and that only the last band is open-ended.
     *
     * @param ordered the bands, ordered by lower bound
     * @throws InvalidTaxScheduleException if they do not fit together
     */
    private static void checkTheBandsCoverAllGrossPayFromZero(List<TaxBand> ordered) {
        if (ordered.isEmpty()) {
            throw new InvalidTaxScheduleException("there must be at least one band");
        }

        TaxBand lowest = ordered.get(0);

        if (!lowest.lowerBound().equals(ZERO)) {
            throw new InvalidTaxScheduleException("the lowest band, " + lowest.name()
                    + ", must start at " + ZERO + " but starts at " + lowest.lowerBound());
        }

        for (int index = 0; index < ordered.size() - 1; index++) {
            checkBandEndsWhereTheNextBegins(ordered.get(index), ordered.get(index + 1));
        }

        TaxBand highest = ordered.get(ordered.size() - 1);
        Optional<Money> highestEnd = highest.upperBound();

        if (highestEnd.isPresent()) {
            throw new InvalidTaxScheduleException("the highest band, " + highest.name()
                    + ", must have no upper limit, but ends at " + highestEnd.get());
        }
    }

    private static void checkBandEndsWhereTheNextBegins(TaxBand band, TaxBand next) {
        Optional<Money> end = band.upperBound();

        if (end.isEmpty()) {
            throw new InvalidTaxScheduleException("band " + band.name()
                    + " has no upper limit, so band " + next.name() + " can never apply");
        }

        if (end.get().isLessThan(next.lowerBound())) {
            throw new InvalidTaxScheduleException("there is a gap between band " + band.name()
                    + ", which ends at " + end.get() + ", and band " + next.name()
                    + ", which starts at " + next.lowerBound());
        }

        if (next.lowerBound().isLessThan(end.get())) {
            throw new InvalidTaxScheduleException("band " + band.name() + ", which ends at " + end.get()
                    + ", overlaps band " + next.name() + ", which starts at " + next.lowerBound());
        }
    }
}
