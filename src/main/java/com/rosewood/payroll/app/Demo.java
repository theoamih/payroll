package com.rosewood.payroll.app;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.netpay.PayrollCalculator;
import com.rosewood.payroll.pay.GrossPayCalculator;
import com.rosewood.payroll.pay.HourlyRate;
import com.rosewood.payroll.pay.Hours;
import com.rosewood.payroll.tax.RosewoodTaxBands;

import java.math.BigDecimal;
import java.util.List;

/**
 * Runs the net-pay service on a few example employees.
 *
 * <p>This is the only place that chooses concrete classes and connects them. The
 * rest of the code receives what it needs through its constructors, so any part can
 * be replaced, in production or in a test, without changing the parts that use it.
 */
public class Demo {

    private final PayrollCalculator calculator;

    private final PayslipPrinter printer;

    /**
     * Holds the two parts every payslip in the demo uses, so each call only states what
     * changes from one payslip to the next. Private, because the demo is only created
     * and run by {@link #main(String[])}.
     */
    private Demo(PayrollCalculator calculator, PayslipPrinter printer) {
        this.calculator = calculator;
        this.printer = printer;
    }

    /**
     * Prints payslips for two known employees, then shows how an unknown employee and
     * invalid hours are refused.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        EmployeeRepository repository = new InMemoryEmployeeRepository(List.of(
                new Employee("e1", "Efua Sarpong"),
                new Employee("e2", "Kwame Owusu")));

        PayrollCalculator calculator = new PayrollCalculator(
                repository, new GrossPayCalculator(), RosewoodTaxBands.schedule());
        PayslipPrinter printer = new PayslipPrinter(System.out, System.err);

        Demo demo = new Demo(calculator, printer);

        demo.printPayslip("e1", hours("40"), rate("20"));
        demo.printPayslip("e2", hours("50"), rate("30"));
        demo.printPayslip("unknown", hours("40"), rate("20"));
        demo.printPayslip("e1", hours("-10"), rate("20"));
    }

    private static Hours hours(String value) {
        return new Hours(new BigDecimal(value));
    }

    private static HourlyRate rate(String value) {
        return new HourlyRate(new BigDecimal(value));
    }

    /**
     * Works out and prints one employee's payslip, or reports why it was refused.
     *
     * <p>Failures are handled here, at the edge of the program, because this is where
     * there is someone to tell. A refused payslip is reported and the remaining
     * payslips are still worked out. Catching {@link PayrollException} covers every
     * failure the service reports on purpose, while a programming error still stops
     * the program instead of being reported as a refused payslip.
     *
     * @param employeeId the employee being paid
     * @param hours      the hours worked
     * @param rate       the pay for one ordinary hour
     */
    private void printPayslip(String employeeId, Hours hours, HourlyRate rate) {
        try {
            printer.print(calculator.calculatePayslip(employeeId, hours, rate));
        } catch (PayrollException e) {
            printer.printRefusal(employeeId, e);
        }
    }
}
