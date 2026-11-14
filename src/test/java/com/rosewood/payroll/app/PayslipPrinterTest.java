package com.rosewood.payroll.app;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeNotFoundException;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.netpay.Payslip;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayslipPrinterTest {

    private static final String NEW_LINE = System.lineSeparator();

    // The printer writes to these instead of the console, so each test can read back
    // exactly what was printed to each stream.
    private final ByteArrayOutputStream payslipOutput = new ByteArrayOutputStream();

    private final ByteArrayOutputStream refusalOutput = new ByteArrayOutputStream();

    private final PayslipPrinter printer = new PayslipPrinter(
            new PrintStream(payslipOutput, true, StandardCharsets.UTF_8),
            new PrintStream(refusalOutput, true, StandardCharsets.UTF_8));

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    private static String textOf(ByteArrayOutputStream output) {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printsWhoThePayslipIsForAndEachLineWithAmountsLinedUp() {
        Payslip payslip = new Payslip(new Employee("e2", "Kwame Owusu"),
                money("1650.00"), "MEDIUM", money("247.50"), money("1402.50"));

        printer.print(payslip);

        String expected = "Payslip for Kwame Owusu (e2)" + NEW_LINE
                + "  Gross pay     GHS 1650.00" + NEW_LINE
                + "  Tax band           MEDIUM" + NEW_LINE
                + "  Tax            GHS 247.50" + NEW_LINE
                + "  Net pay       GHS 1402.50" + NEW_LINE;

        assertEquals(expected, textOf(payslipOutput));
        assertEquals("", textOf(refusalOutput));
    }

    // Refusals go to their own stream, so a run that prints many payslips can show
    // the failures separately and they are not lost among the payslips.
    @Test
    void reportsARefusalWithItsReasonOnTheRefusalStream() {
        printer.printRefusal("unknown", new EmployeeNotFoundException("unknown"));

        assertEquals("Payslip for unknown refused: No employee found with id 'unknown'" + NEW_LINE,
                textOf(refusalOutput));
        assertEquals("", textOf(payslipOutput));
    }

    @Test
    void refusesAMissingStream() {
        PrintStream stream = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        assertThrows(NullPointerException.class, () -> new PayslipPrinter(null, stream));
        assertThrows(NullPointerException.class, () -> new PayslipPrinter(stream, null));
    }
}
