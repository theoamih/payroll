package com.rosewood.payroll.employee;

import com.rosewood.payroll.PayrollException;

import java.io.Serial;

/**
 * Thrown when pay is requested for an employee id that has no employee record.
 */
public class EmployeeNotFoundException extends PayrollException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The id that was looked up and not found. */
    private final String employeeId;

    /**
     * Creates the exception for an id that has no employee record.
     *
     * @param employeeId the id that was looked up and not found
     */
    public EmployeeNotFoundException(String employeeId) {
        super("No employee found with id '" + employeeId + "'");
        this.employeeId = employeeId;
    }

    /**
     * Gives the missing id as its own value, so callers can act on it without
     * having to pick it out of the message.
     *
     * @return the id that was looked up and not found
     */
    public String getEmployeeId() {
        return employeeId;
    }
}
