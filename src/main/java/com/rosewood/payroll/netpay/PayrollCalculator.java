package com.rosewood.payroll.netpay;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.GrossPayCalculator;
import com.rosewood.payroll.pay.InvalidPayInputException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Works out an employee's payslip from the hours they worked and their hourly rate.
 *
 * <p>It coordinates the steps rather than doing them: gross pay is worked out by a
 * {@link GrossPayCalculator}, and employees are looked up in an
 * {@link EmployeeRepository}.
 */
public class PayrollCalculator {

    private final EmployeeRepository repository;

    private final GrossPayCalculator grossPayCalculator;

    /**
     * Creates a calculator from the parts it coordinates.
     *
     * @param repository         where employee records are looked up
     * @param grossPayCalculator works out gross pay from hours and rate
     * @throws NullPointerException if either argument is null
     */
    public PayrollCalculator(EmployeeRepository repository, GrossPayCalculator grossPayCalculator) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.grossPayCalculator = Objects.requireNonNull(grossPayCalculator, "grossPayCalculator must not be null");
    }

    /**
     * Works out an employee's payslip for the hours worked.
     *
     * <p>Nothing is printed here. The payslip is returned, so the caller decides how to
     * show it, and tests can check every line of it.
     *
     * @param employeeId  the employee being paid
     * @param hoursWorked the hours worked in the pay period
     * @param hourlyRate  the pay for one ordinary hour, at any precision
     * @return the payslip, with gross pay, tax and net pay in whole pesewas
     * @throws InvalidPayInputException  if hoursWorked is negative or hourlyRate is not
     *                                   above zero
     * @throws EmployeeNotFoundException if no employee has the given id
     * @throws NullPointerException      if any argument is null
     */
    public Payslip calculatePayslip(String employeeId, BigDecimal hoursWorked, BigDecimal hourlyRate)
            throws InvalidPayInputException, EmployeeNotFoundException {

        Objects.requireNonNull(employeeId, "employeeId must not be null");

        // The timesheet is checked first, so a bad timesheet is reported as what it is
        // and the employee store is not touched for a payslip that cannot be produced.
        Money gross = grossPayCalculator.calculate(hoursWorked, hourlyRate);

        // No payslip is produced for someone who is not on record.
        Employee employee = repository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        // tax band, worked out from gross pay
        BigDecimal taxRate;
        if (gross.isLessThan(new Money(new BigDecimal("1000")))) {
            taxRate = new BigDecimal("0.05");      // LOW
        } else if (gross.isLessThan(new Money(new BigDecimal("5000")))) {
            taxRate = new BigDecimal("0.15");      // MEDIUM
        } else {
            taxRate = new BigDecimal("0.25");      // HIGH
        }

        // Tax is rounded to pesewas on its own and net is gross minus tax, so gross,
        // tax and net on a payslip always add up exactly.
        Money tax = gross.times(taxRate);
        Money net = gross.minus(tax);

        return new Payslip(employee, gross, tax, net);
    }
}
