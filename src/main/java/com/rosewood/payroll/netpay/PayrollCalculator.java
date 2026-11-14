package com.rosewood.payroll.netpay;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;

import java.util.Objects;

/**
 * Works out an employee's net pay from the hours they worked and their hourly rate.
 */
public class PayrollCalculator {

    private final EmployeeRepository repository;

    /**
     * @param repository where employee records are looked up
     * @throws NullPointerException if repository is null
     */
    public PayrollCalculator(EmployeeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Works out an employee's net pay for the hours worked.
     *
     * @param employeeId  the employee being paid
     * @param hoursWorked the hours worked in the pay period
     * @param hourlyRate  the pay for one ordinary hour
     * @return the net pay, after tax
     * @throws EmployeeNotFoundException if no employee has the given id
     * @throws NullPointerException      if employeeId is null
     */
    public double calculateNetPay(String employeeId, double hoursWorked, double hourlyRate)
            throws EmployeeNotFoundException {

        Objects.requireNonNull(employeeId, "employeeId must not be null");

        // Checked before any pay is worked out, so pay is never calculated for someone
        // who is not on record.
        Employee employee = repository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        double gross = hoursWorked * hourlyRate;

        // overtime: extra hours beyond 40 are paid at time-and-a-half
        if (hoursWorked > 40) {
            gross = gross + (hoursWorked - 40) * hourlyRate * 0.5;
        }

        // tax band, worked out from gross pay
        double taxRate;
        if (gross < 1000) {
            taxRate = 0.05;      // LOW
        } else if (gross < 5000) {
            taxRate = 0.15;      // MEDIUM
        } else {
            taxRate = 0.25;      // HIGH
        }

        double net = gross - (gross * taxRate);
        System.out.println("Net pay for " + employee.getName() + ": GHS " + net);
        return net;
    }
}
