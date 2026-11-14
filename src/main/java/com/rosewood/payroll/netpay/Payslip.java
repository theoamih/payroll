package com.rosewood.payroll.netpay;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.money.Money;

import java.util.Objects;

/**
 * The result of working out one employee's pay: gross pay, the tax band that applied,
 * the tax taken, and the net pay that is left.
 *
 * <p>A payslip whose lines do not add up cannot be created, so no code can hand
 * one on, however it was produced.
 *
 * @param employee the employee being paid
 * @param grossPay pay before tax, in whole pesewas
 * @param taxBand  the name of the tax band that applied to gross pay
 * @param tax      the tax taken from gross pay, in whole pesewas
 * @param netPay   gross pay minus tax, in whole pesewas
 */
public record Payslip(Employee employee, Money grossPay, String taxBand, Money tax, Money netPay) {

    /**
     * Checks that every line is present and that net pay is exactly gross pay minus tax.
     *
     * @throws NullPointerException     if any line is null
     * @throws IllegalArgumentException if net pay is not gross pay minus tax
     */
    public Payslip {
        Objects.requireNonNull(employee, "employee must not be null");
        Objects.requireNonNull(grossPay, "grossPay must not be null");
        Objects.requireNonNull(taxBand, "taxBand must not be null");
        Objects.requireNonNull(tax, "tax must not be null");
        Objects.requireNonNull(netPay, "netPay must not be null");

        if (!grossPay.minus(tax).equals(netPay)) {
            throw new IllegalArgumentException(
                    "net pay " + netPay + " must equal gross pay " + grossPay + " minus tax " + tax);
        }
    }
}
