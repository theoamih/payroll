package com.rosewood.payroll.netpay;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.pay.InvalidPayInputException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Specifies the net pay PayrollCalculator must produce and the failures it must report.
 *
 * <p>The expected amounts were worked out by hand, so they show the pay rules
 * independently of the code, and they protect those rules while the code is refactored.
 *
 * <p>Tests tagged {@code known-defect} describe behaviour the code does not have yet,
 * so they fail. The tag keeps them out of the default build, and they can be run on
 * their own with {@code mvn test -Pknown-defects}. The tag is removed when the defect is
 * fixed. C1 to C4 refer to the problems described in ANALYSIS.md.
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

        // A null repository would otherwise only fail later, inside calculateNetPay,
        // far from the code that made the mistake. It is refused straight away.
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
        void taxesGrossPayAtTheRateOfItsBand(double hours, double rate, String band, double expectedNet)
                throws PayrollException {

            double net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);

            assertEquals(expectedNet, net, FLOATING_POINT_TOLERANCE);
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
        void paysHoursOverFortyAtTimeAndAHalf(double hours, double rate, String rule, double expectedNet)
                throws PayrollException {

            double net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);

            assertEquals(expectedNet, net, FLOATING_POINT_TOLERANCE);
        }
    }

    // Zero is a real answer for someone who did not work, so it must stay distinct from
    // the failures below, which the starter code also reported as zero.
    @Test
    void paysNothingForZeroHoursWorked() throws PayrollException {
        double net = calculator.calculateNetPay(KNOWN_EMPLOYEE, 0, 20);

        assertEquals(0.0, net, FLOATING_POINT_TOLERANCE);
    }

    @Nested
    class MissingEmployee {

        // C2: pay must never be worked out for someone with no employee record, and the
        // caller needs the id to report which record is missing.
        @Test
        void rejectsAnEmployeeWhoDoesNotExist() {
            EmployeeNotFoundException error = assertThrows(EmployeeNotFoundException.class,
                    () -> calculator.calculateNetPay("unknown", 40, 20),
                    "an employee who does not exist must be rejected, not paid");

            assertEquals("unknown", error.getEmployeeId());
            assertEquals("No employee found with id 'unknown'", error.getMessage());
        }

        // C2: a null id is a mistake in the calling code, not a missing record, so it is
        // reported as a NullPointerException rather than as EmployeeNotFoundException.
        @Test
        void rejectsAMissingEmployeeId() {
            assertThrows(NullPointerException.class, () -> calculator.calculateNetPay(null, 40, 20),
                    "a missing employee id must be rejected, not paid");
        }
    }

    @Nested
    class FailingRepository {

        // C1: a catch-all would turn a broken employee store into a payslip that looks
        // valid. This guards against one ever being put back around the lookup.
        @Test
        void letsALookupFailureReachTheCallerInsteadOfPayingZero() {
            IllegalStateException failure = new IllegalStateException("employee store unavailable");

            EmployeeRepository repository = mock(EmployeeRepository.class);
            when(repository.findById(KNOWN_EMPLOYEE)).thenThrow(failure);

            PayrollCalculator calculatorWithFailingStore = new PayrollCalculator(repository);

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> calculatorWithFailingStore.calculateNetPay(KNOWN_EMPLOYEE, 40, 20));

            // The same exception object, not a replacement, so no detail about the
            // original failure is lost on the way to the caller.
            assertSame(failure, thrown);
        }
    }

    @Nested
    class InvalidInput {

        // C3: negative hours would produce negative pay. The exception names the field
        // and the value, so whoever fixes the timesheet knows exactly what to correct.
        @Test
        void rejectsNegativeHours() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, -10, 20),
                    "negative hours must be rejected");

            assertEquals("hoursWorked", error.getField());
            assertEquals(-10.0, error.getRejectedValue());
            assertEquals("hoursWorked must be a finite number of zero or more, but was -10.0", error.getMessage());
        }

        // C3: the smallest step below zero is refused too, so the check is "below zero",
        // not "well below zero".
        @Test
        void rejectsHoursJustBelowZero() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, -0.01, 20));

            assertEquals("hoursWorked", error.getField());
        }

        // C3: hours that are not a number would produce pay that is not a number.
        @Test
        void rejectsHoursThatAreNotANumber() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, Double.NaN, 20),
                    "hours that are not a number must be rejected");

            assertEquals("hoursWorked", error.getField());
        }

        // C3: infinite hours would produce pay that is not a number.
        @Test
        void rejectsInfiniteHours() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, Double.POSITIVE_INFINITY, 20),
                    "infinite hours must be rejected");

            assertEquals("hoursWorked", error.getField());
        }

        // C3: a negative rate would produce negative pay.
        @Test
        void rejectsANegativeRate() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, 40, -20),
                    "a negative hourly rate must be rejected");

            assertEquals("hourlyRate", error.getField());
            assertEquals(-20.0, error.getRejectedValue());
            assertEquals("hourlyRate must be a finite number greater than zero, but was -20.0", error.getMessage());
        }

        // A zero rate would quietly pay nothing for real work, which looks like a valid
        // payslip. It is almost certainly a data-entry mistake, so it is refused.
        @Test
        void rejectsAZeroRate() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, 40, 0));

            assertEquals("hourlyRate", error.getField());
        }

        @ParameterizedTest(name = "rate {0}")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
        void rejectsARateThatIsNotAFiniteNumber(double rate) {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, 40, rate));

            assertEquals("hourlyRate", error.getField());
        }

        // Invalid input is refused before the employee is looked up, so a payslip that
        // cannot be produced never touches the employee store.
        @Test
        void rejectsInvalidInputBeforeLookingUpTheEmployee() {
            EmployeeRepository repository = mock(EmployeeRepository.class);

            PayrollCalculator calculatorWithMockStore = new PayrollCalculator(repository);

            assertThrows(InvalidPayInputException.class,
                    () -> calculatorWithMockStore.calculateNetPay(KNOWN_EMPLOYEE, -10, 20));

            verifyNoInteractions(repository);
        }
    }

    @Nested
    class Rounding {

        // C4: a payslip can only show whole pesewas. The exact amount depends on the
        // rounding rule, which has not been chosen yet, so this checks only the precision.
        @ParameterizedTest(name = "{0}h at {1}")
        @Tag(KNOWN_DEFECT)
        @CsvSource({
                "41,   0.10",
                "37.5, 13.33",
        })
        void paysAnAmountInWholePesewas(double hours, double rate) throws PayrollException {
            double net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);

            int decimalPlaces = BigDecimal.valueOf(net).stripTrailingZeros().scale();

            assertTrue(decimalPlaces <= 2, () -> "net pay must be a whole number of pesewas, but was " + net);
        }
    }
}
