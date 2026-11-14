package com.rosewood.payroll.pay;

import com.rosewood.payroll.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Works out gross pay, before tax, from the hours worked and the hourly rate.
 *
 * <p>Hours above the overtime threshold in the pay period are paid at time-and-a-half.
 */
public class GrossPayCalculator {

    /** Hours above this, in one pay period, are paid as overtime. */
    private static final BigDecimal OVERTIME_THRESHOLD_HOURS = new BigDecimal("40");

    /**
     * The extra part of the hourly rate paid for each overtime hour. Every hour is
     * already paid at the ordinary rate, so an extra half makes overtime
     * time-and-a-half.
     */
    private static final BigDecimal OVERTIME_PREMIUM = new BigDecimal("0.5");

    /**
     * Creates a calculator. It holds no state, so one instance can be shared.
     */
    public GrossPayCalculator() {
    }

    /**
     * Works out gross pay, rounded to whole pesewas.
     *
     * <p>Base pay and overtime are added together before rounding, so gross pay is
     * rounded only once.
     *
     * @param hoursWorked the hours worked in the pay period
     * @param hourlyRate  the pay for one ordinary hour, at any precision
     * @return gross pay, in whole pesewas
     * @throws InvalidPayInputException if hoursWorked is negative or hourlyRate is not
     *                                  above zero
     * @throws NullPointerException     if either argument is null
     */
    public Money calculate(BigDecimal hoursWorked, BigDecimal hourlyRate) throws InvalidPayInputException {
        Objects.requireNonNull(hoursWorked, "hoursWorked must not be null");
        Objects.requireNonNull(hourlyRate, "hourlyRate must not be null");

        validateHoursWorked(hoursWorked);
        validateHourlyRate(hourlyRate);

        BigDecimal unroundedGross = hoursWorked.multiply(hourlyRate);

        if (hoursWorked.compareTo(OVERTIME_THRESHOLD_HOURS) > 0) {
            BigDecimal overtimeHours = hoursWorked.subtract(OVERTIME_THRESHOLD_HOURS);
            BigDecimal overtimePremium = overtimeHours.multiply(hourlyRate).multiply(OVERTIME_PREMIUM);

            unroundedGross = unroundedGross.add(overtimePremium);
        }

        return new Money(unroundedGross);
    }

    /**
     * Zero hours is allowed, because an employee may not have worked in the period.
     * Negative hours are not, because they would produce negative pay.
     *
     * @param hoursWorked the hours to check
     * @throws InvalidPayInputException if the hours are negative
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
     *
     * @param hourlyRate the rate to check
     * @throws InvalidPayInputException if the rate is zero or negative
     */
    private static void validateHourlyRate(BigDecimal hourlyRate) throws InvalidPayInputException {
        if (hourlyRate.signum() <= 0) {
            throw new InvalidPayInputException("hourlyRate", hourlyRate, "greater than zero");
        }
    }
}
