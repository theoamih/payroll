package com.rosewood.payroll;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollCalculatorTest {

    // Pay is still a double, so amounts are compared with a tolerance that only
    // absorbs floating-point noise. It is far smaller than one pesewa.
    private static final double FLOATING_POINT_TOLERANCE = 1e-9;

    @Test
    void calculatesNetPay() {
        PayrollCalculator calculator = new PayrollCalculator(
                new InMemoryEmployeeRepository(List.of(new Employee("e1", "Efua Sarpong"))));
        double net = calculator.calculateNetPay("e1", 40, 20);
        // 40 h at 20 is a gross of 800, in the LOW band (5%): 800 - 40 = 760
        assertEquals(760.0, net, FLOATING_POINT_TOLERANCE);
    }

    @Test
    void calculatesOvertimePay() {
        // Mock the dependency, not the class under test, so the real pay calculation runs.
        EmployeeRepository repository = mock(EmployeeRepository.class);
        when(repository.findById("e2")).thenReturn(new Employee("e2", "Kwame Owusu"));
        PayrollCalculator calculator = new PayrollCalculator(repository);

        double net = calculator.calculateNetPay("e2", 50, 30);

        // 50 h at 30: 1500, plus 10 overtime hours x 30 x 0.5 = 150, is a gross of 1650,
        // in the MEDIUM band (15%): 1650 - 247.50 = 1402.50
        assertEquals(1402.5, net, FLOATING_POINT_TOLERANCE);
    }

    @Test
    void returnsPayForAnEmployee() {
        PayrollCalculator calculator = new PayrollCalculator(
                new InMemoryEmployeeRepository(List.of(new Employee("e1", "Efua Sarpong"))));
        double net = calculator.calculateNetPay("e1", 35, 25);
        // 35 h at 25 is a gross of 875, in the LOW band (5%): 875 - 43.75 = 831.25
        assertEquals(831.25, net, FLOATING_POINT_TOLERANCE);
    }
}
