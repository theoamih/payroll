package com.rosewood.payroll.netpay;

import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.money.Money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayslipTest {

    private final Employee efua = new Employee("e1", "Efua Sarpong");

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    @Test
    void holdsLinesThatAddUp() {
        Payslip payslip = new Payslip(efua, money("800.00"), "LOW", money("40.00"), money("760.00"));

        assertEquals(money("800.00"), payslip.grossPay());
        assertEquals("LOW", payslip.taxBand());
        assertEquals(money("40.00"), payslip.tax());
        assertEquals(money("760.00"), payslip.netPay());
    }

    // A payslip whose lines do not add up is wrong whatever produced it, so it cannot
    // be created at all. One pesewa out is enough to be refused.
    @Test
    void refusesLinesThatDoNotAddUp() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Payslip(efua, money("800.00"), "LOW", money("40.00"), money("760.01")));

        assertEquals("net pay GHS 760.01 must equal gross pay GHS 800.00 minus tax GHS 40.00", error.getMessage());
    }

    @Test
    void refusesAMissingLine() {
        Money gross = money("800.00");
        Money tax = money("40.00");
        Money net = money("760.00");

        assertThrows(NullPointerException.class, () -> new Payslip(null, gross, "LOW", tax, net));
        assertThrows(NullPointerException.class, () -> new Payslip(efua, null, "LOW", tax, net));
        assertThrows(NullPointerException.class, () -> new Payslip(efua, gross, null, tax, net));
        assertThrows(NullPointerException.class, () -> new Payslip(efua, gross, "LOW", null, net));
        assertThrows(NullPointerException.class, () -> new Payslip(efua, gross, "LOW", tax, null));
    }
}
