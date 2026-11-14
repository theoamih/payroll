package com.rosewood.payroll.pay;

import com.rosewood.payroll.money.Money;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrossPayCalculatorTest {

    private final GrossPayCalculator grossPayCalculator = new GrossPayCalculator();

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    private static Hours hours(String value) {
        return new Hours(new BigDecimal(value));
    }

    private static HourlyRate rate(String value) {
        return new HourlyRate(new BigDecimal(value));
    }

    @ParameterizedTest(name = "{2}: {0}h at {1} -> gross {3}")
    @CsvSource({
            "0,    20, no hours,                                     0.00",
            "37.5, 20, ordinary hours only,                          750.00",
            "40,   20, exactly 40 hours earns no overtime,           800.00",
            "40.5, 20, half an hour over 40 adds half a rate x 0.5,  815.00",
            "41,   20, one hour over 40 adds 10 at time-and-a-half,  830.00",
            "50,   30, ten hours over 40 add 150 at time-and-a-half, 1650.00",
    })
    void paysOrdinaryHoursAtTheRateAndHoursOverFortyAtTimeAndAHalf(
            BigDecimal hours, BigDecimal rate, String rule, String expectedGross) throws InvalidPayInputException {

        Money gross = grossPayCalculator.calculate(new Hours(hours), new HourlyRate(rate));

        assertEquals(money(expectedGross), gross);
    }

    // Rounding each part before adding them would give 1.22 + 0.01 = 1.23. Adding first
    // and rounding once gives 1.2225 -> 1.22, so this pins the rule that gross pay is
    // rounded only once:
    //   40.5h at 0.03: base 1.215 + overtime 0.5 x 0.03 x 0.5 = 0.0075 -> 1.2225 -> 1.22
    @Test
    void roundsGrossPayOnceAfterAddingBasePayAndOvertime() throws InvalidPayInputException {
        Money gross = grossPayCalculator.calculate(hours("40.5"), rate("0.03"));

        assertEquals(money("1.22"), gross);
    }

    @Test
    void rejectsNegativeHours() {
        InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                () -> grossPayCalculator.calculate(hours("-0.01"), rate("20")));

        assertEquals("hoursWorked", error.getField());
        assertEquals("hoursWorked must be zero or more, but was -0.01", error.getMessage());
    }

    @Test
    void rejectsARateThatIsNotAboveZero() {
        InvalidPayInputException zeroRate = assertThrows(InvalidPayInputException.class,
                () -> grossPayCalculator.calculate(hours("40"), rate("0")));
        InvalidPayInputException negativeRate = assertThrows(InvalidPayInputException.class,
                () -> grossPayCalculator.calculate(hours("40"), rate("-0.01")));

        assertEquals("hourlyRate", zeroRate.getField());
        assertEquals("hourlyRate", negativeRate.getField());
    }

    @Test
    void rejectsMissingHoursOrRate() {
        assertThrows(NullPointerException.class, () -> grossPayCalculator.calculate(null, rate("20")));
        assertThrows(NullPointerException.class, () -> grossPayCalculator.calculate(hours("40"), null));
    }
}
