package com.rosewood.payroll.netpay;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The result of working out one employee's pay: gross pay, the tax band that applied,
 * the tax taken, and the net pay that is left.
 *
 * <p>A payslip whose lines do not add up cannot be created, so no code can hand
 * one on, however it was produced.
 *
 * <p>Tax must also be between zero and gross pay. Each tax band works out its own
 * tax, and a new band with its own rule could get that wrong, for example by taking a
 * fixed amount that is more than a small gross. Checking here means a band like that
 * fails on the first payslip it touches, instead of producing negative pay. Together
 * with the first check, it also means neither gross pay nor net pay can be negative.
 *
 * @param employee the employee being paid
 * @param grossPay pay before tax, in whole pesewas
 * @param taxBand  the name of the tax band that applied to gross pay
 * @param tax      the tax taken from gross pay, in whole pesewas
 * @param netPay   gross pay minus tax, in whole pesewas
 */
public record Payslip(Employee employee, Money grossPay, String taxBand, Money tax, Money netPay) {

    private static final Money ZERO = new Money(BigDecimal.ZERO);

    /**
     * Checks that every line is present, that tax is between zero and gross pay, and
     * that net pay is exactly gross pay minus tax.
     *
     * @throws NullPointerException     if any line is null
     * @throws IllegalArgumentException if tax is negative or more than gross pay, or if
     *                                  net pay is not gross pay minus tax
     */
    public Payslip {
        Objects.requireNonNull(employee, "employee must not be null");
        Objects.requireNonNull(grossPay, "grossPay must not be null");
        Objects.requireNonNull(taxBand, "taxBand must not be null");
        Objects.requireNonNull(tax, "tax must not be null");
        Objects.requireNonNull(netPay, "netPay must not be null");

        // Zero tax and tax of all the gross pay are both allowed, because a band may
        // have a rate of 0 or 1.
        if (tax.isLessThan(ZERO)) {
            throw new IllegalArgumentException("tax " + tax + " must not be negative");
        }

        if (grossPay.isLessThan(tax)) {
            throw new IllegalArgumentException(
                    "tax " + tax + " must not be more than gross pay " + grossPay);
        }

        if (!grossPay.minus(tax).equals(netPay)) {
            throw new IllegalArgumentException(
                    "net pay " + netPay + " must equal gross pay " + grossPay + " minus tax " + tax);
        }
    }
}
