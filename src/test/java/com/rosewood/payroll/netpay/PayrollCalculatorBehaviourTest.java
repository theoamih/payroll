package com.rosewood.payroll.netpay;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.GrossPayCalculator;
import com.rosewood.payroll.pay.HourlyRate;
import com.rosewood.payroll.pay.Hours;
import com.rosewood.payroll.pay.InvalidPayInputException;
import com.rosewood.payroll.tax.RosewoodTaxBands;
import com.rosewood.payroll.tax.TaxSchedule;

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
 * <p>C1 to C4 refer to the problems described in ANALYSIS.md.
 */
class PayrollCalculatorBehaviourTest {

    private static final String KNOWN_EMPLOYEE = "e1";

    private static final Hours FORTY_HOURS = hours("40");

    private static final HourlyRate RATE_OF_TWENTY = rate("20");

    private final PayrollCalculator calculator = calculatorWith(
            new InMemoryEmployeeRepository(List.of(new Employee(KNOWN_EMPLOYEE, "Efua Sarpong"))));

    // Every test builds its calculator here, so a test only states the part it cares
    // about, the employee store, and the real pay rules are always the ones under test.
    private static PayrollCalculator calculatorWith(EmployeeRepository repository) {
        return new PayrollCalculator(repository, new GrossPayCalculator(), RosewoodTaxBands.schedule());
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    private static Hours hours(String value) {
        return new Hours(new BigDecimal(value));
    }

    private static HourlyRate rate(String value) {
        return new HourlyRate(new BigDecimal(value));
    }

    @Nested
    class Construction {

        // A missing part would otherwise only fail later, inside calculatePayslip, far
        // from the code that made the mistake. It is refused straight away.
        @Test
        void refusesAMissingPart() {
            EmployeeRepository repository = new InMemoryEmployeeRepository(List.of());
            GrossPayCalculator grossPayCalculator = new GrossPayCalculator();
            TaxSchedule taxSchedule = RosewoodTaxBands.schedule();

            assertThrows(NullPointerException.class,
                    () -> new PayrollCalculator(null, grossPayCalculator, taxSchedule));
            assertThrows(NullPointerException.class,
                    () -> new PayrollCalculator(repository, null, taxSchedule));
            assertThrows(NullPointerException.class,
                    () -> new PayrollCalculator(repository, grossPayCalculator, null));
        }
    }

    @Nested
    class TaxBands {

        @ParameterizedTest(name = "{2}: {0}h at {1} -> {3} band, net {4}")
        @CsvSource({
                "40, 20,      LOW band,                     LOW,    760.00",
                "37, 27,      LOW band top (gross 999),     LOW,    949.05",
                "40, 25,      MEDIUM band starts at 1000,   MEDIUM, 850.00",
                "40, 124.975, MEDIUM band top (gross 4999), MEDIUM, 4249.15",
                "40, 125,     HIGH band starts at 5000,     HIGH,   3750.00",
                "40, 200,     HIGH band,                    HIGH,   6000.00",
        })
        void taxesGrossPayAtTheRateOfItsBand(BigDecimal hours, BigDecimal rate, String description,
                                             String expectedBand, String expectedNet) throws PayrollException {

            Payslip payslip = calculator.calculatePayslip(KNOWN_EMPLOYEE, new Hours(hours), new HourlyRate(rate));

            assertEquals(expectedBand, payslip.taxBand());
            assertEquals(money(expectedNet), payslip.netPay());
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

            Money net = calculator.calculatePayslip(KNOWN_EMPLOYEE, new Hours(hours), new HourlyRate(rate)).netPay();

            assertEquals(money(expectedNet), net);
        }
    }

    // Zero is a real answer for someone who did not work, so it must stay distinct from
    // the failures below, which the starter code also reported as zero.
    @Test
    void paysNothingForZeroHoursWorked() throws PayrollException {
        Money net = calculator.calculatePayslip(KNOWN_EMPLOYEE, hours("0"), RATE_OF_TWENTY).netPay();

        assertEquals(money("0.00"), net);
    }

    @Nested
    class PayslipLines {

        // Every line of the payslip is checked, not only net pay, because a payslip with
        // the right net but a wrong gross or tax is still a wrong payslip.
        //   50h at 30: 1500 + 10 overtime hours x 30 x 0.5 = 1650 gross,
        //   MEDIUM band (15%) tax 247.50, net 1402.50
        @Test
        void showsTheEmployeeGrossPayTaxAndNetPay() throws PayrollException {
            Employee kwame = new Employee("e2", "Kwame Owusu");

            PayrollCalculator calculatorForKwame = calculatorWith(new InMemoryEmployeeRepository(List.of(kwame)));

            Payslip payslip = calculatorForKwame.calculatePayslip("e2", hours("50"), rate("30"));

            assertSame(kwame, payslip.employee());
            assertEquals(money("1650.00"), payslip.grossPay());
            assertEquals("MEDIUM", payslip.taxBand());
            assertEquals(money("247.50"), payslip.tax());
            assertEquals(money("1402.50"), payslip.netPay());
        }
    }

    @Nested
    class MissingEmployee {

        // C2: pay must never be worked out for someone with no employee record, and the
        // caller needs the id to report which record is missing.
        @Test
        void rejectsAnEmployeeWhoDoesNotExist() {
            EmployeeNotFoundException error = assertThrows(EmployeeNotFoundException.class,
                    () -> calculator.calculatePayslip("unknown", FORTY_HOURS, RATE_OF_TWENTY),
                    "an employee who does not exist must be rejected, not paid");

            assertEquals("unknown", error.getEmployeeId());
            assertEquals("No employee found with id 'unknown'", error.getMessage());
        }

        // C2: a null id is a mistake in the calling code, not a missing record, so it is
        // reported as a NullPointerException rather than as EmployeeNotFoundException.
        @Test
        void rejectsAMissingEmployeeId() {
            assertThrows(NullPointerException.class,
                    () -> calculator.calculatePayslip(null, FORTY_HOURS, RATE_OF_TWENTY),
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

            PayrollCalculator calculatorWithFailingStore = calculatorWith(repository);

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> calculatorWithFailingStore.calculatePayslip(KNOWN_EMPLOYEE, FORTY_HOURS, RATE_OF_TWENTY));

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
                    () -> calculator.calculatePayslip(KNOWN_EMPLOYEE, hours("-10"), RATE_OF_TWENTY),
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
                    () -> calculator.calculatePayslip(KNOWN_EMPLOYEE, hours("-0.01"), RATE_OF_TWENTY));

            assertEquals("hoursWorked", error.getField());
        }

        // C3: a negative rate would produce negative pay.
        @Test
        void rejectsANegativeRate() {
            InvalidPayInputException error = assertThrows(InvalidPayInputException.class,
                    () -> calculator.calculatePayslip(KNOWN_EMPLOYEE, FORTY_HOURS, rate("-20")),
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
                    () -> calculator.calculatePayslip(KNOWN_EMPLOYEE, FORTY_HOURS, rate("0")));

            assertEquals("hourlyRate", error.getField());
        }

        // Missing hours or a missing rate is a mistake in the calling code, not bad
        // timesheet data, so like a missing id it is a NullPointerException.
        @Test
        void rejectsMissingHoursOrRate() {
            assertThrows(NullPointerException.class,
                    () -> calculator.calculatePayslip(KNOWN_EMPLOYEE, null, RATE_OF_TWENTY));
            assertThrows(NullPointerException.class,
                    () -> calculator.calculatePayslip(KNOWN_EMPLOYEE, FORTY_HOURS, null));
        }

        // Invalid input is refused before the employee is looked up, so a payslip that
        // cannot be produced never touches the employee store.
        @Test
        void rejectsInvalidInputBeforeLookingUpTheEmployee() {
            EmployeeRepository repository = mock(EmployeeRepository.class);

            PayrollCalculator calculatorWithMockStore = calculatorWith(repository);

            assertThrows(InvalidPayInputException.class,
                    () -> calculatorWithMockStore.calculatePayslip(KNOWN_EMPLOYEE, hours("-10"), RATE_OF_TWENTY));

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

            Money net = calculator.calculatePayslip(KNOWN_EMPLOYEE, new Hours(hours), new HourlyRate(rate)).netPay();

            assertEquals(money(expectedNet), net);
        }

        // 1h at 0.10 gives a gross of 0.10 and tax of exactly half a pesewa (0.005).
        // Rounding half up makes the tax 0.01, so net is 0.09. Banker's rounding would
        // have made the tax 0.00, so this test pins the chosen rule.
        @Test
        void roundsHalfAPesewaOfTaxUp() throws PayrollException {
            Money net = calculator.calculatePayslip(KNOWN_EMPLOYEE, hours("1"), rate("0.10")).netPay();

            assertEquals(money("0.09"), net);
        }
    }
}
