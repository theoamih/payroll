package com.rosewood.payroll.netpay;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.GrossPayCalculator;
import com.rosewood.payroll.pay.HourlyRate;
import com.rosewood.payroll.pay.Hours;
import com.rosewood.payroll.tax.RosewoodTaxBands;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks rules that must hold for every valid timesheet, not just for the examples
 * the other tests work out by hand.
 *
 * <p>Each rule is checked on a fixed grid: every hours value below combined with every
 * rate below. The values are written out here, so every run uses the same inputs and
 * anyone can see which cases are covered.
 *
 * <p>One rule is deliberately missing: "more gross pay never means less net pay". It
 * is false on purpose, because the whole gross is taxed at its band's rate. A gross of
 * 999.99 nets 949.99, but a gross of 1000.00 nets 850.00. That cliff is pinned by the
 * band-edge examples in {@link PayrollCalculatorBehaviourTest}.
 */
class PayRulesThatAlwaysHoldTest {

    private static final String EMPLOYEE = "e1";

    // In ascending order, which the hours rule below depends on.
    //   0              no work in the period
    //   0.5            part of an hour
    //   39.99, 40,     either side of, and exactly on, the overtime threshold
    //   40.01
    //   41, 60         overtime
    //   168            every hour in a week, as a very large value, since no maximum is set
    private static final List<String> HOURS = List.of(
            "0", "0.5", "39.99", "40", "40.01", "41", "60", "168");

    //   0.01           so small that some pay rounds to zero pesewas
    //   1, 20          ordinary rates
    //   24.99975       at 40 hours, a gross of 999.99, the top of LOW
    //   25             at 40 hours, a gross of 1000.00, the start of MEDIUM
    //   124.975        at 40 hours, a gross of 4999.00, near the top of MEDIUM, and a
    //                  rate that must not be rounded
    //   125            at 40 hours, a gross of 5000.00, the start of HIGH
    //   1000           a very large rate
    private static final List<String> RATES = List.of(
            "0.01", "1", "20", "24.99975", "25", "124.975", "125", "1000");

    private static final Money ZERO = new Money(BigDecimal.ZERO);

    private final PayrollCalculator calculator = new PayrollCalculator(
            new InMemoryEmployeeRepository(List.of(new Employee(EMPLOYEE, "Efua Sarpong"))),
            new GrossPayCalculator(),
            RosewoodTaxBands.schedule());

    static Stream<Arguments> everyHoursValueWithEveryRate() {
        return HOURS.stream()
                .flatMap(hours -> RATES.stream().map(rate -> Arguments.of(hours, rate)));
    }

    static Stream<String> everyRate() {
        return RATES.stream();
    }

    private Payslip payslipFor(String hours, String rate) throws PayrollException {
        return calculator.calculatePayslip(
                EMPLOYEE, new Hours(new BigDecimal(hours)), new HourlyRate(new BigDecimal(rate)));
    }

    // The real bands must produce a payslip for every input, and on it tax can never
    // be negative or take more than the gross pay, so net pay is never negative either.
    @ParameterizedTest(name = "{0}h at {1}")
    @MethodSource("everyHoursValueWithEveryRate")
    void taxIsNeverNegativeAndNeverMoreThanGrossPay(String hours, String rate) throws PayrollException {
        Payslip payslip = payslipFor(hours, rate);

        assertFalse(payslip.tax().isLessThan(ZERO),
                () -> "tax " + payslip.tax() + " is negative");
        assertFalse(payslip.grossPay().isLessThan(payslip.tax()),
                () -> "tax " + payslip.tax() + " is more than gross pay " + payslip.grossPay());
        assertFalse(payslip.netPay().isLessThan(ZERO),
                () -> "net pay " + payslip.netPay() + " is negative");
    }

    // At any one rate, working longer must never earn less. Gross pay can stay the same
    // between two close hours values once it is rounded to pesewas, for example 39.99h
    // and 40h at 0.01 are both 0.40, so the rule is "never less", not "always more".
    @ParameterizedTest(name = "at a rate of {0}")
    @MethodSource("everyRate")
    void moreHoursNeverMeansLessGrossPay(String rate) throws PayrollException {
        Money previousGross = ZERO;
        String previousHours = "0";

        for (String hours : HOURS) {
            Money gross = payslipFor(hours, rate).grossPay();

            Money grossForFewerHours = previousGross;
            String fewerHours = previousHours;

            assertFalse(gross.isLessThan(grossForFewerHours),
                    () -> hours + "h earns " + gross + ", less than the " + grossForFewerHours
                            + " earned for " + fewerHours + "h");

            previousGross = gross;
            previousHours = hours;
        }
    }
}
