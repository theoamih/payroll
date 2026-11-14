package com.rosewood.payroll;

import java.util.List;

public class Demo {
    public static void main(String[] args) {
        EmployeeRepository repository = new InMemoryEmployeeRepository(List.of(
                new Employee("e1", "Efua Sarpong"),
                new Employee("e2", "Kwame Owusu")));
        PayrollCalculator calculator = new PayrollCalculator(repository);

        printNetPay(calculator, "Result e1, 40h @ 20    ", "e1", 40, 20);
        printNetPay(calculator, "Result e2, 50h @ 30    ", "e2", 50, 30);
        printNetPay(calculator, "Result unknown employee", "unknown", 40, 20);
    }

    // The edge of the program: a refused payslip is reported, and the remaining
    // payslips are still worked out.
    private static void printNetPay(PayrollCalculator calculator, String label,
                                    String employeeId, double hours, double rate) {
        try {
            System.out.println(label + " -> " + calculator.calculateNetPay(employeeId, hours, rate));
        } catch (EmployeeNotFoundException e) {
            System.err.println(label + " -> refused: " + e.getMessage());
        }
    }
}
