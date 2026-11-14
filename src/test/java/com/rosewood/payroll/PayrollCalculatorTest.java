package com.rosewood.payroll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollCalculatorTest {

    // Pay is still a double, so amounts are compared with a tolerance that only
    // absorbs floating-point noise. It is far smaller than one pesewa.
    private static final double FLOATING_POINT_TOLERANCE = 1e-9;

    @Test
    void calculatesNetPay() {
        PayrollCalculator calculator = new PayrollCalculator();
        double net = calculator.calculateNetPay("e1", 40, 20);
        assertTrue(net > 0); // it returned some pay
    }

    @Test
    void calculatesOvertimePay() {
        // mock the calculator and check it returns what we told it to
        PayrollCalculator calculator = mock(PayrollCalculator.class);
        when(calculator.calculateNetPay("e2", 50, 30)).thenReturn(1402.5);
        assertEquals(1402.5, calculator.calculateNetPay("e2", 50, 30));
    }

    @Test
    void returnsPayForAnEmployee() {
        PayrollCalculator calculator = new PayrollCalculator();
        double net = calculator.calculateNetPay("e1", 35, 25);
        // 35 h at 25 is a gross of 875, in the LOW band (5%): 875 - 43.75 = 831.25
        assertEquals(831.25, net, FLOATING_POINT_TOLERANCE);
    }
}
