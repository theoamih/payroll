package com.rosewood.payroll.app;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.netpay.PayrollCalculator;

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
     * Prints net pay for two known employees and one that does not exist.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        EmployeeRepository repository = new InMemoryEmployeeRepository(List.of(
                new Employee("e1", "Efua Sarpong"),
                new Employee("e2", "Kwame Owusu")));

        PayrollCalculator calculator = new PayrollCalculator(repository);

        printNetPay(calculator, "Result e1, 40h @ 20    ", "e1", 40, 20);
        printNetPay(calculator, "Result e2, 50h @ 30    ", "e2", 50, 30);
        printNetPay(calculator, "Result unknown employee", "unknown", 40, 20);
    }

    /**
     * Prints one employee's net pay, or the reason it was refused.
     *
     * <p>Failures are handled here, at the edge of the program, because this is where
     * there is someone to tell. A refused payslip is reported on the error stream and
     * the remaining payslips are still worked out.
     *
     * @param calculator the service that works out net pay
     * @param label      the text printed before the result
     * @param employeeId the employee being paid
     * @param hours      the hours worked
     * @param rate       the pay for one ordinary hour
     */
    private static void printNetPay(PayrollCalculator calculator, String label,
                                    String employeeId, double hours, double rate) {
        try {
            double net = calculator.calculateNetPay(employeeId, hours, rate);

            System.out.println(label + " -> " + net);
        } catch (EmployeeNotFoundException e) {
            System.err.println(label + " -> refused: " + e.getMessage());
        }
    }
}
