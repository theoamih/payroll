package com.rosewood.payroll.employee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeTest {

    @Test
    void holdsTheIdAndName() {
        Employee employee = new Employee("e1", "Efua Sarpong");

        assertEquals("e1", employee.getId());
        assertEquals("Efua Sarpong", employee.getName());
    }

    // Without this check, an employee with no name was accepted and printed as
    // "Payslip for null (e1)".
    @Test
    void refusesAMissingIdOrName() {
        NullPointerException missingId = assertThrows(NullPointerException.class,
                () -> new Employee(null, "Efua Sarpong"));
        NullPointerException missingName = assertThrows(NullPointerException.class,
                () -> new Employee("e1", null));

        assertEquals("id must not be null", missingId.getMessage());
        assertEquals("name must not be null", missingName.getMessage());
    }

    // Spaces or a tab alone are as empty as no text at all.
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", " ", "\t"})
    void refusesABlankIdOrName(String blank) {
        IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                () -> new Employee(blank, "Efua Sarpong"));
        IllegalArgumentException blankName = assertThrows(IllegalArgumentException.class,
                () -> new Employee("e1", blank));

        assertEquals("id must not be blank", blankId.getMessage());
        assertEquals("name must not be blank", blankName.getMessage());
    }
}
