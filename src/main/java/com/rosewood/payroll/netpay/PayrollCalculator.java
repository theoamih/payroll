package com.rosewood.payroll.netpay;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.InvalidPayInputException;

import java.math.BigDecimal;
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
     * @param hourlyRate  the pay for one ordinary hour, at any precision
     * @return the net pay, after tax, in whole pesewas
     * @throws InvalidPayInputException  if hoursWorked is negative or hourlyRate is not
     *                                   above zero
     * @throws EmployeeNotFoundException if no employee has the given id
     * @throws NullPointerException      if any argument is null
     */
    public Money calculateNetPay(String employeeId, BigDecimal hoursWorked, BigDecimal hourlyRate)
            throws InvalidPayInputException, EmployeeNotFoundException {

        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(hoursWorked, "hoursWorked must not be null");
        Objects.requireNonNull(hourlyRate, "hourlyRate must not be null");

        // Checked before the lookup, so a bad timesheet is reported as what it is and
        // the employee store is not touched for a payslip that cannot be produced.
        validateHoursWorked(hoursWorked);
        validateHourlyRate(hourlyRate);

        // Checked before any pay is worked out, so pay is never calculated for someone
        // who is not on record.
        Employee employee = repository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        BigDecimal unroundedGross = hoursWorked.multiply(hourlyRate);

        // overtime: extra hours beyond 40 are paid at time-and-a-half
        if (hoursWorked.compareTo(new BigDecimal("40")) > 0) {
            BigDecimal overtimeHours = hoursWorked.subtract(new BigDecimal("40"));
            BigDecimal overtimePremium = overtimeHours.multiply(hourlyRate).multiply(new BigDecimal("0.5"));

            unroundedGross = unroundedGross.add(overtimePremium);
        }

        // Rounded to pesewas once, after base pay and overtime are added together, so
        // gross pay is never rounded twice.
        Money gross = new Money(unroundedGross);

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

        System.out.println("Net pay for " + employee.getName() + ": " + net);
        return net;
    }

    /**
     * Zero hours is allowed, because an employee may not have worked in the period.
     * Negative hours are not, because they would produce negative pay.
     */
    private static void validateHoursWorked(BigDecimal hoursWorked) throws InvalidPayInputException {
        if (hoursWorked.signum() < 0) {
            throw new InvalidPayInputException("hoursWorked", hoursWorked, "zero or more");
        }
    }

    /**
     * A rate of zero is refused as well as a negative one. Nobody is employed at
     * GHS 0 an hour, so a zero rate is almost certainly a data-entry mistake, and
     * paying 0 without complaint is exactly the kind of believable wrong payslip
     * this service must not produce.
     */
    private static void validateHourlyRate(BigDecimal hourlyRate) throws InvalidPayInputException {
        if (hourlyRate.signum() <= 0) {
            throw new InvalidPayInputException("hourlyRate", hourlyRate, "greater than zero");
        }
    }
}
