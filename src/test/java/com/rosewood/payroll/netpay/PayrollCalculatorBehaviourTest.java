package com.rosewood.payroll.netpay;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.InvalidPayInputException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private static final String KNOWN_EMPLOYEE = "e1";

    private static final BigDecimal FORTY_HOURS = new BigDecimal("40");

    private static final BigDecimal RATE_OF_TWENTY = new BigDecimal("20");

    private final PayrollCalculator calculator = new PayrollCalculator(
            new InMemoryEmployeeRepository(List.of(new Employee(KNOWN_EMPLOYEE, "Efua Sarpong"))));

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

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
        void taxesGrossPayAtTheRateOfItsBand(BigDecimal hours, BigDecimal rate, String band, String expectedNet)
                throws PayrollException {

            Money net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);

            assertEquals(money(expectedNet), net);
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
        void paysHoursOverFortyAtTimeAndAHalf(BigDecimal hours, BigDecimal rate, String rule, String expectedNet)
                throws PayrollException {

            Money net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);

            assertEquals(money(expectedNet), net);
        }
    }

    // Zero is a real answer for someone who did not work, so it must stay distinct from
    // the failures below, which the starter code also reported as zero.
    @Test
    void paysNothingForZeroHoursWorked() throws PayrollException {
        Money net = calculator.calculateNetPay(KNOWN_EMPLOYEE, BigDecimal.ZERO, RATE_OF_TWENTY);

        assertEquals(money("0.00"), net);
    }

    @Nested
    class MissingEmployee {

        // C2: pay must never be worked out for someone with no employee record, and the
        // caller needs the id to report which record is missing.
        @Test
        void rejectsAnEmployeeWhoDoesNotExist() {
            EmployeeNotFoundException error = assertThrows(EmployeeNotFoundException.class,
                    () -> calculator.calculateNetPay("unknown", FORTY_HOURS, RATE_OF_TWENTY),
                    "an employee who does not exist must be rejected, not paid");

            assertEquals("unknown", error.getEmployeeId());
            assertEquals("No employee found with id 'unknown'", error.getMessage());
        }

        // C2: a null id is a mistake in the calling code, not a missing record, so it is
        // reported as a NullPointerException rather than as EmployeeNotFoundException.
        @Test
        void rejectsAMissingEmployeeId() {
            assertThrows(NullPointerException.class,
                    () -> calculator.calculateNetPay(null, FORTY_HOURS, RATE_OF_TWENTY),
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
                    () -> calculatorWithFailingStore.calculateNetPay(KNOWN_EMPLOYEE, FORTY_HOURS, RATE_OF_TWENTY));

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
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, new BigDecimal("-10"), RATE_OF_TWENTY),
                    "negative hours must be rejected");

            assertEquals("hoursWorked", error.getField());
            assertEquals(new BigDecimal("-10"), error.getRejectedValue());
            assertEquals("hoursWorked must be zero or more, but was -10", error.getMessage());
        }

        // C3: the smallest step below zero is refused too, so the check is "below zero",
        // not "well below zero".
        @Test
        void rejectsHoursJustBelowZero() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, new BigDecimal("-0.01"), RATE_OF_TWENTY));

            assertEquals("hoursWorked", error.getField());
        }

        // C3: a negative rate would produce negative pay.
        @Test
        void rejectsANegativeRate() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, FORTY_HOURS, new BigDecimal("-20")),
                    "a negative hourly rate must be rejected");

            assertEquals("hourlyRate", error.getField());
            assertEquals(new BigDecimal("-20"), error.getRejectedValue());
            assertEquals("hourlyRate must be greater than zero, but was -20", error.getMessage());
        }

        // A zero rate would quietly pay nothing for real work, which looks like a valid
        // payslip. It is almost certainly a data-entry mistake, so it is refused.
        @Test
        void rejectsAZeroRate() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, FORTY_HOURS, BigDecimal.ZERO));

            assertEquals("hourlyRate", error.getField());
        }

        // Missing hours or a missing rate is a mistake in the calling code, not bad
        // timesheet data, so like a missing id it is a NullPointerException.
        @Test
        void rejectsMissingHoursOrRate() {
            assertThrows(NullPointerException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, null, RATE_OF_TWENTY));
            assertThrows(NullPointerException.class,
                    () -> calculator.calculateNetPay(KNOWN_EMPLOYEE, FORTY_HOURS, null));
        }

        // Invalid input is refused before the employee is looked up, so a payslip that
        // cannot be produced never touches the employee store.
        @Test
        void rejectsInvalidInputBeforeLookingUpTheEmployee() {
            EmployeeRepository repository = mock(EmployeeRepository.class);

            PayrollCalculator calculatorWithMockStore = new PayrollCalculator(repository);

            assertThrows(InvalidPayInputException.class,
                    () -> calculatorWithMockStore.calculateNetPay(KNOWN_EMPLOYEE, new BigDecimal("-10"), RATE_OF_TWENTY));

            verifyNoInteractions(repository);
        }
    }

    @Nested
    class Rounding {

        // C4: a payslip can only show whole pesewas. Gross is rounded once and tax on its
        // own, then net is gross minus tax.
        //   41h at 0.10:    gross 4.10 + 0.05 overtime = 4.15, tax 0.2075 -> 0.21, net 3.94
        //   37.5h at 13.33: gross 499.875 -> 499.88, tax 24.994 -> 24.99, net 474.89
        @ParameterizedTest(name = "{0}h at {1} -> net {2}")
        @CsvSource({
                "41,   0.10,  3.94",
                "37.5, 13.33, 474.89",
        })
        void paysAnAmountInWholePesewas(BigDecimal hours, BigDecimal rate, String expectedNet)
                throws PayrollException {

            Money net = calculator.calculateNetPay(KNOWN_EMPLOYEE, hours, rate);

            assertEquals(money(expectedNet), net);
        }

        // 1h at 0.10 gives a gross of 0.10 and tax of exactly half a pesewa (0.005).
        // Rounding half up makes the tax 0.01, so net is 0.09. Banker's rounding would
        // have made the tax 0.00, so this test pins the chosen rule.
        @Test
        void roundsHalfAPesewaOfTaxUp() throws PayrollException {
            Money net = calculator.calculateNetPay(KNOWN_EMPLOYEE, BigDecimal.ONE, new BigDecimal("0.10"));

            assertEquals(money("0.09"), net);
        }
    }
}
