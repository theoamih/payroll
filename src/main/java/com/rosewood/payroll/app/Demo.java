package com.rosewood.payroll.app;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.netpay.PayrollCalculator;

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

    /**
     * Not meant to be created. The demo is run through {@link #main(String[])}.
     */
    private Demo() {
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

        PayrollCalculator calculator = new PayrollCalculator(repository);
        PayslipPrinter printer = new PayslipPrinter(System.out, System.err);

        printPayslip(calculator, printer, "e1", new BigDecimal("40"), new BigDecimal("20"));
        printPayslip(calculator, printer, "e2", new BigDecimal("50"), new BigDecimal("30"));
        printPayslip(calculator, printer, "unknown", new BigDecimal("40"), new BigDecimal("20"));
        printPayslip(calculator, printer, "e1", new BigDecimal("-10"), new BigDecimal("20"));
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
     * @param calculator the service that works out the payslip
     * @param printer    where the payslip or the refusal is printed
     * @param employeeId the employee being paid
     * @param hours      the hours worked
     * @param rate       the pay for one ordinary hour
     */
    private static void printPayslip(PayrollCalculator calculator, PayslipPrinter printer,
                                     String employeeId, BigDecimal hours, BigDecimal rate) {
        try {
            printer.print(calculator.calculatePayslip(employeeId, hours, rate));
        } catch (PayrollException e) {
            printer.printRefusal(employeeId, e);
        }
    }
}
