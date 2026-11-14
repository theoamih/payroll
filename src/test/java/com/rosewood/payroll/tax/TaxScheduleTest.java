package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaxScheduleTest {

    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    private static TaxBand band(String name, String lowerBound, String upperBound) {
        return FlatRateTaxBand.between(name, money(lowerBound), money(upperBound), TEN_PERCENT);
    }

    private static TaxBand openBand(String name, String lowerBound) {
        return FlatRateTaxBand.startingAt(name, money(lowerBound), TEN_PERCENT);
    }

    /**
     * A band that declares whatever range it is given, with no checks of its own, like a
     * new TaxBand implementation might. FlatRateTaxBand cannot be used for this, because
     * it refuses a range that does not end above where it starts.
     */
    private record UncheckedRangeTaxBand(String name, Money lowerBound, Optional<Money> upperBound)
            implements TaxBand {

        @Override
        public Money taxOn(Money grossPay) {
            return money("0");
        }
    }

    @Nested
    class ChoosingABand {

        // Each band includes its lower bound and stops just before its upper bound, so
        // the amounts either side of each edge land in different bands.
        @ParameterizedTest(name = "gross {0} -> {1}")
        @CsvSource({
                "0.00,       LOW",
                "999.99,     LOW",
                "1000.00,    MEDIUM",
                "4999.99,    MEDIUM",
                "5000.00,    HIGH",
                "1000000.00, HIGH",
        })
        void choosesTheBandWhoseRangeIncludesTheGrossPay(String gross, String expectedBand) {
            TaxSchedule schedule = RosewoodTaxBands.schedule();

            TaxBand band = schedule.bandFor(money(gross));

            assertEquals(expectedBand, band.name());
        }

        @Test
        void choosesTheSameBandWhateverOrderTheBandsWereGivenIn() {
            TaxSchedule schedule = new TaxSchedule(List.of(
                    RosewoodTaxBands.HIGH, RosewoodTaxBands.LOW, RosewoodTaxBands.MEDIUM));

            assertSame(RosewoodTaxBands.MEDIUM, schedule.bandFor(money("1000.00")));
        }

        // Every amount from zero up is covered, so only a negative amount, which gross
        // pay can never be, has no band. It must fail loudly, never fall back to a band.
        @Test
        void failsLoudlyForANegativeAmount() {
            TaxSchedule schedule = RosewoodTaxBands.schedule();

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> schedule.bandFor(money("-0.01")));

            assertEquals("no tax band covers gross pay of GHS -0.01", error.getMessage());
        }

        // The schedule keeps its own copy, so changing the list it was built from
        // afterwards cannot change which band is chosen.
        @Test
        void isNotChangedByLaterChangesToTheListItWasGiven() {
            List<TaxBand> bands = new ArrayList<>(List.of(
                    RosewoodTaxBands.LOW, RosewoodTaxBands.MEDIUM, RosewoodTaxBands.HIGH));

            TaxSchedule schedule = new TaxSchedule(bands);
            bands.clear();

            assertSame(RosewoodTaxBands.LOW, schedule.bandFor(money("10.00")));
        }
    }

    // A schedule is checked when it is created, so a badly set-up band is found when the
    // program starts, not on payday when someone's pay falls into the gap it leaves.
    @Nested
    class CheckingTheBandsWhenCreated {

        @Test
        void acceptsASingleBandThatCoversEverything() {
            TaxSchedule schedule = new TaxSchedule(List.of(openBand("FLAT", "0")));

            assertEquals("FLAT", schedule.bandFor(money("123.45")).name());
        }

        @Test
        void refusesAScheduleWithNoBands() {
            List<TaxBand> noBands = List.of();

            InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(noBands));

            assertEquals("invalid tax schedule: there must be at least one band", error.getMessage());
        }

        @Test
        void refusesBandsThatDoNotStartAtZero() {
            List<TaxBand> bands = List.of(band("LOW", "100", "1000"), openBand("HIGH", "1000"));

            InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(bands));

            assertEquals("invalid tax schedule: the lowest band, LOW, must start at GHS 0.00"
                    + " but starts at GHS 100.00", error.getMessage());
        }

        @Test
        void refusesAGapBetweenTwoBands() {
            List<TaxBand> bands = List.of(band("LOW", "0", "1000"), openBand("HIGH", "1200"));

            InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(bands));

            assertEquals("invalid tax schedule: there is a gap between band LOW, which ends at GHS 1000.00,"
                    + " and band HIGH, which starts at GHS 1200.00", error.getMessage());
        }

        @Test
        void refusesTwoBandsThatOverlap() {
            List<TaxBand> bands = List.of(band("LOW", "0", "1200"), openBand("HIGH", "1000"));

            InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(bands));

            assertEquals("invalid tax schedule: band LOW, which ends at GHS 1200.00,"
                    + " overlaps band HIGH, which starts at GHS 1000.00", error.getMessage());
        }

        // Two bands starting at the same amount both claim it, so this is an overlap too.
        @Test
        void refusesTwoBandsThatStartAtTheSameAmount() {
            List<TaxBand> bands = List.of(band("LOW", "0", "1000"), openBand("ALSO LOW", "0"));

            assertThrows(InvalidTaxScheduleException.class, () -> new TaxSchedule(bands));
        }

        // A band from 1000 up to 1000 covers nothing. Placed between two bands that meet at
        // 1000, it touches both neighbours exactly, so without a check of its own it would
        // be accepted and silently never apply. It is refused in either order.
        @Test
        void refusesABandThatDoesNotEndAboveWhereItStarts() {
            TaxBand empty = new UncheckedRangeTaxBand("EMPTY", money("1000"), Optional.of(money("1000")));
            TaxBand low = band("LOW", "0", "1000");
            TaxBand high = openBand("HIGH", "1000");

            InvalidTaxScheduleException emptyListedFirst = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(List.of(low, empty, high)));
            InvalidTaxScheduleException emptyListedLast = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(List.of(low, high, empty)));

            String expected = "invalid tax schedule: band EMPTY must end above where it starts,"
                    + " but starts at GHS 1000.00 and ends at GHS 1000.00";

            assertEquals(expected, emptyListedFirst.getMessage());
            assertEquals(expected, emptyListedLast.getMessage());
        }

        @Test
        void refusesAnOpenEndedBandThatIsNotTheHighest() {
            List<TaxBand> bands = List.of(openBand("LOW", "0"), openBand("HIGH", "1000"));

            InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(bands));

            assertEquals("invalid tax schedule: band LOW has no upper limit,"
                    + " so band HIGH can never apply", error.getMessage());
        }

        @Test
        void refusesAHighestBandWithAnUpperLimit() {
            List<TaxBand> bands = List.of(band("LOW", "0", "1000"), band("HIGH", "1000", "9000"));

            InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                    () -> new TaxSchedule(bands));

            assertEquals("invalid tax schedule: the highest band, HIGH, must have no upper limit,"
                    + " but ends at GHS 9000.00", error.getMessage());
        }

        @Test
        void refusesAMissingListOrBand() {
            List<TaxBand> bandsWithANull = Arrays.asList(RosewoodTaxBands.LOW, null);

            assertThrows(NullPointerException.class, () -> new TaxSchedule(null));
            assertThrows(NullPointerException.class, () -> new TaxSchedule(bandsWithANull));
        }
    }
}
