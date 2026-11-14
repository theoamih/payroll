package com.rosewood.payroll;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private final PayrollCalculator calculator = new PayrollCalculator();

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
}
