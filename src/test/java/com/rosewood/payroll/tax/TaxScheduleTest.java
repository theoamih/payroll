package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaxScheduleTest {

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    // Each band includes its lower bound and stops just before its upper bound, so the
    // amounts either side of each edge land in different bands.
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

    // Until the schedule checks its bands when it is created, a gap is only found when a
    // gross amount falls into it. It must then fail loudly, never fall back to a band.
    @Test
    void failsLoudlyWhenNoBandCoversTheGrossPay() {
        TaxSchedule scheduleWithAGap = new TaxSchedule(List.of(RosewoodTaxBands.MEDIUM, RosewoodTaxBands.HIGH));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> scheduleWithAGap.bandFor(money("500.00")));

        assertEquals("no tax band covers gross pay of GHS 500.00", error.getMessage());
    }

    // The schedule keeps its own copy, so changing the list it was built from afterwards
    // cannot change which band is chosen.
    @Test
    void isNotChangedByLaterChangesToTheListItWasGiven() {
        List<TaxBand> bands = new ArrayList<>(List.of(
                RosewoodTaxBands.LOW, RosewoodTaxBands.MEDIUM, RosewoodTaxBands.HIGH));

        TaxSchedule schedule = new TaxSchedule(bands);
        bands.clear();

        assertSame(RosewoodTaxBands.LOW, schedule.bandFor(money("10.00")));
    }

    @Test
    void refusesAMissingListOrBand() {
        List<TaxBand> bandsWithANull = Arrays.asList(RosewoodTaxBands.LOW, null);

        assertThrows(NullPointerException.class, () -> new TaxSchedule(null));
        assertThrows(NullPointerException.class, () -> new TaxSchedule(bandsWithANull));
    }
}
