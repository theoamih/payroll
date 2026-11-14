package com.rosewood.payroll;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the net pay that PayrollCalculator actually returns, written before any
 * production code was changed. The expected amounts were worked out by hand and
 * match what the starter code returns, so these tests protect the pay rules
 * during the refactor.
 */
class PayrollCalculatorBehaviourTest {

    // Pay is still a double at this stage, so amounts are compared with a tolerance
    // that only absorbs floating-point noise. It is far smaller than one pesewa.
    private static final double FLOATING_POINT_TOLERANCE = 1e-9;

    private static final String KNOWN_EMPLOYEE = "e1";

    private static final String KNOWN_DEFECT = "known-defect";

    private final PayrollCalculator calculator = new PayrollCalculator(
            new InMemoryEmployeeRepository(List.of(new Employee(KNOWN_EMPLOYEE, "Efua Sarpong"))));

    @Nested
    class Construction {

        // A null repository would fail inside calculateNetPay, where the catch-all (C1)
        // would turn it into 0 pay for everyone. It must be refused straight away.
        @Test
        void refusesAMissingRepository() {
            assertThrows(NullPointerException.class, () -> new PayrollCalculator(null));
        }
    }

    @Nested
    class TaxBands {

        @ParameterizedTest(name = "{2}: {0}h at {1} -> net {3}")
        @CsvSource({
                "40, 20,      LOW band,                     760.00",
                "37, 27,      LOW band top (gross 999),     949.05",
                "40, 25,      MEDIUM band starts at 1000,   850.00",
                "40, 124.975, MEDIUM band top (gross 4999), 4249.15",
                "40, 125,     HIGH band starts at 5000,     3750.00",
                "40, 200,     HIGH band,                    6000.00",
        })
        void taxesGrossPayAtTheRateOfItsBand(double hours, double rate, String band, double expectedNet) {
            assertEquals(expectedNet, calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate),
                    FLOATING_POINT_TOLERANCE);
        }
    }

    @Nested
    class Overtime {

        @ParameterizedTest(name = "{2}: {0}h at {1} -> net {3}")
        @CsvSource({
                "40,   20, exactly 40 hours earns no overtime,              760.00",
                "40.5, 20, half an hour over 40 is paid at time-and-a-half, 774.25",
                "41,   20, one hour over 40 is paid at time-and-a-half,     788.50",
                "50,   30, ten hours over 40 are paid at time-and-a-half,   1402.50",
        })
        void paysHoursOverFortyAtTimeAndAHalf(double hours, double rate, String rule, double expectedNet) {
            assertEquals(expectedNet, calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate),
                    FLOATING_POINT_TOLERANCE);
        }
    }

    @Test
    void paysNothingForZeroHoursWorked() {
        assertEquals(0.0, calculator.calculateNetPay(KNOWN_EMPLOYEE, 0, 20), FLOATING_POINT_TOLERANCE);
    }

    // The tests below describe behaviour the starter code does not have yet, so they fail
    // on it. Each carries the known-defect tag, which keeps it out of the default build
    // (run them with: mvn test -Pknown-defects). The tag is removed when the defect is fixed.
    // C1 to C4 refer to the problems in ANALYSIS.md.

    @Nested
    class MissingEmployee {

        // C1, C2
        @Test
        @Tag(KNOWN_DEFECT)
        void rejectsAnEmployeeWhoDoesNotExist() {
            assertThrows(Exception.class, () -> calculator.calculateNetPay("unknown", 40, 20),
                    "an employee who does not exist must be rejected, not paid");
        }

        // C1, C2
        @Test
        @Tag(KNOWN_DEFECT)
        void rejectsAMissingEmployeeId() {
            assertThrows(Exception.class, () -> calculator.calculateNetPay(null, 40, 20),
                    "a missing employee id must be rejected, not paid");
        }
    }

    @Nested
    class InvalidInput {

        // C3
        @Test
        @Tag(KNOWN_DEFECT)
        void rejectsNegativeHours() {
            assertThrows(Exception.class, () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, -10, 20),
                    "negative hours must be rejected");
        }

        // C3
        @Test
        @Tag(KNOWN_DEFECT)
        void rejectsANegativeRate() {
            assertThrows(Exception.class, () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, 40, -20),
                    "a negative hourly rate must be rejected");
        }

        // C3
        @Test
        @Tag(KNOWN_DEFECT)
        void rejectsHoursThatAreNotANumber() {
            assertThrows(Exception.class, () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, Double.NaN, 20),
                    "hours that are not a number must be rejected");
        }

        // C3
        @Test
        @Tag(KNOWN_DEFECT)
        void rejectsInfiniteHours() {
            assertThrows(Exception.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, Double.POSITIVE_INFINITY, 20),
                    "infinite hours must be rejected");
        }
    }

    @Nested
    class Rounding {

        // C4. The exact amount depends on the rounding rule, which has not been chosen yet,
        // so this checks only that the result is a whole number of pesewas.
        @ParameterizedTest(name = "{0}h at {1}")
        @Tag(KNOWN_DEFECT)
        @CsvSource({
                "41,   0.10",
                "37.5, 13.33",
        })
        void paysAnAmountInWholePesewas(double hours, double rate) {
            double net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);
            int decimalPlaces = BigDecimal.valueOf(net).stripTrailingZeros().scale();
            assertTrue(decimalPlaces <= 2, () -> "net pay must be a whole number of pesewas, but was " + net);
        }
    }
}
