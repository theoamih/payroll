package com.rosewood.payroll.app;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.netpay.Payslip;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Turns payslips, and the reasons payslips were refused, into text.
 *
 * <p>Presentation is kept here, apart from the pay calculation, so the layout can
 * change without touching how pay is worked out. The output streams are passed in
 * rather than taken from {@code System.out}, so tests can capture and check exactly
 * what is printed.
 */
public class PayslipPrinter {

    private final PrintStream payslips;

    private final PrintStream refusals;

    /**
     * Creates a printer that writes payslips and refusals to separate streams.
     *
     * @param payslips where payslips are printed
     * @param refusals where refused payslips are reported
     * @throws NullPointerException if either stream is null
     */
    public PayslipPrinter(PrintStream payslips, PrintStream refusals) {
        this.payslips = Objects.requireNonNull(payslips, "payslips must not be null");
        this.refusals = Objects.requireNonNull(refusals, "refusals must not be null");
    }

    /**
     * Prints a payslip: who it is for, then gross pay, the tax band, tax and net pay.
     *
     * @param payslip the payslip to print
     */
    public void print(Payslip payslip) {
        payslips.println("Payslip for " + payslip.employee().getName() + " (" + payslip.employee().getId() + ")");
        payslips.println(line("Gross pay", payslip.grossPay()));
        payslips.println(line("Tax band", payslip.taxBand()));
        payslips.println(line("Tax", payslip.tax()));
        payslips.println(line("Net pay", payslip.netPay()));
    }

    /**
     * Reports that an employee's payslip could not be produced, and why.
     *
     * @param employeeId the employee whose payslip was refused
     * @param reason     the failure that stopped it
     */
    public void printRefusal(String employeeId, PayrollException reason) {
        refusals.println("Payslip for " + employeeId + " refused: " + reason.getMessage());
    }

    /**
     * Lays out one line of a payslip with the label on the left and the value lined
     * up on the right, so the values on a payslip sit under each other.
     */
    private static String line(String label, Object value) {
        return String.format("  %-10s%15s", label, value);
    }
}
