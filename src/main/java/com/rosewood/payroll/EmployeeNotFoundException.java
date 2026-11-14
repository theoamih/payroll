package com.rosewood.payroll;

import java.io.Serial;

/**
 * Thrown when pay is requested for an employee id that has no employee record.
 */
public class EmployeeNotFoundException extends PayrollException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String employeeId;

    public EmployeeNotFoundException(String employeeId) {
        super("No employee found with id '" + employeeId + "'");
        this.employeeId = employeeId;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
