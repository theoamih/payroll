package com.rosewood.payroll.netpay;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.EmployeeRepository;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.GrossPayCalculator;
import com.rosewood.payroll.tax.RosewoodTaxBands;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollCalculatorTest {

    @Test
    void calculatesNetPay() throws PayrollException {
        PayrollCalculator calculator = new PayrollCalculator(
                new InMemoryEmployeeRepository(List.of(new Employee("e1", "Efua Sarpong"))),
                new GrossPayCalculator(),
                RosewoodTaxBands.schedule());

        Money net = calculator.calculatePayslip("e1", new BigDecimal("40"), new BigDecimal("20")).netPay();

        // 40 h at 20 is a gross of 800, in the LOW band (5%): 800 - 40 = 760
        assertEquals(new Money(new BigDecimal("760.00")), net);
    }

    @Test
    void calculatesOvertimePay() throws PayrollException {
        // Mock the dependency, not the class under test, so the real pay calculation runs.
        EmployeeRepository repository = mock(EmployeeRepository.class);
        when(repository.findById("e2")).thenReturn(Optional.of(new Employee("e2", "Kwame Owusu")));

        PayrollCalculator calculator = new PayrollCalculator(
                repository,
                new GrossPayCalculator(),
                RosewoodTaxBands.schedule());

        Money net = calculator.calculatePayslip("e2", new BigDecimal("50"), new BigDecimal("30")).netPay();

        // 50 h at 30: 1500, plus 10 overtime hours x 30 x 0.5 = 150, is a gross of 1650,
        // in the MEDIUM band (15%): 1650 - 247.50 = 1402.50
        assertEquals(new Money(new BigDecimal("1402.50")), net);
    }

    @Test
    void returnsPayForAnEmployee() throws PayrollException {
        PayrollCalculator calculator = new PayrollCalculator(
                new InMemoryEmployeeRepository(List.of(new Employee("e1", "Efua Sarpong"))),
                new GrossPayCalculator(),
                RosewoodTaxBands.schedule());

        Money net = calculator.calculatePayslip("e1", new BigDecimal("35"), new BigDecimal("25")).netPay();

        // 35 h at 25 is a gross of 875, in the LOW band (5%): 875 - 43.75 = 831.25
        assertEquals(new Money(new BigDecimal("831.25")), net);
    }
}
