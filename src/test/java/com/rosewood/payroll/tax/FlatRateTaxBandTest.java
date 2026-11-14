package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlatRateTaxBandTest {

    private static final BigDecimal FIFTEEN_PERCENT = new BigDecimal("0.15");

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    @Test
    void declaresTheRangeItWasGiven() {
        FlatRateTaxBand band = FlatRateTaxBand.between("MEDIUM", money("1000"), money("5000"), FIFTEEN_PERCENT);

        assertEquals("MEDIUM", band.name());
        assertEquals(money("1000"), band.lowerBound());
        assertEquals(Optional.of(money("5000")), band.upperBound());
    }

    @Test
    void declaresNoUpperLimitWhenItHasNone() {
        FlatRateTaxBand band = FlatRateTaxBand.startingAt("HIGH", money("5000"), new BigDecimal("0.25"));

        assertEquals(Optional.empty(), band.upperBound());
    }

    // The whole gross is taxed at the band's rate, not just the part above the band's
    // lower bound: 15% of 1650 is 247.50, not 15% of 650.
    @Test
    void taxesTheWholeGrossPayAtItsRate() {
        FlatRateTaxBand band = FlatRateTaxBand.between("MEDIUM", money("1000"), money("5000"), FIFTEEN_PERCENT);

        assertEquals(money("247.50"), band.taxOn(money("1650.00")));
    }

    @Test
    void roundsTheTaxToPesewas() {
        FlatRateTaxBand band = FlatRateTaxBand.between("LOW", money("0"), money("1000"), new BigDecimal("0.05"));

        // 5% of 499.88 is 24.994, which rounds to 24.99
        assertEquals(money("24.99"), band.taxOn(money("499.88")));
    }

    // A rate above 1 would take more tax than the gross pay itself, and a negative rate
    // would add money. Either is a mistake in setting up the band, so it is refused.
    @Test
    void refusesARateOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> FlatRateTaxBand.startingAt("BAD", money("0"), new BigDecimal("1.01")));
        assertThrows(IllegalArgumentException.class,
                () -> FlatRateTaxBand.startingAt("BAD", money("0"), new BigDecimal("-0.01")));
    }

    @Test
    void acceptsTheRatesAtEachEndOfTheRange() {
        FlatRateTaxBand taxFree = FlatRateTaxBand.startingAt("TAX FREE", money("0"), BigDecimal.ZERO);
        FlatRateTaxBand everything = FlatRateTaxBand.startingAt("ALL", money("0"), BigDecimal.ONE);

        assertEquals(money("0.00"), taxFree.taxOn(money("100")));
        assertEquals(money("100.00"), everything.taxOn(money("100")));
    }

    @Test
    void refusesAnUpperBoundThatIsNotAboveTheLowerBound() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> FlatRateTaxBand.between("BAD", money("1000"), money("1000"), FIFTEEN_PERCENT));

        assertEquals("upper bound GHS 1000.00 must be above lower bound GHS 1000.00", error.getMessage());
    }

    @Test
    void refusesABlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> FlatRateTaxBand.startingAt(" ", money("0"), FIFTEEN_PERCENT));
    }

    @Test
    void refusesMissingDetails() {
        assertThrows(NullPointerException.class,
                () -> FlatRateTaxBand.startingAt(null, money("0"), FIFTEEN_PERCENT));
        assertThrows(NullPointerException.class,
                () -> FlatRateTaxBand.startingAt("BAD", null, FIFTEEN_PERCENT));
        assertThrows(NullPointerException.class,
                () -> FlatRateTaxBand.startingAt("BAD", money("0"), null));
        assertThrows(NullPointerException.class,
                () -> FlatRateTaxBand.between("BAD", money("0"), null, FIFTEEN_PERCENT));
    }
}
