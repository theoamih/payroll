package com.rosewood.payroll.employee;

/**
 * A person on the payroll.
 */
public class Employee {

    private final String id;

    private final String name;

    /**
     * Creates an employee record.
     *
     * @param id   the identifier used to look the employee up
     * @param name the employee's name as it appears on a payslip
     */
    public Employee(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the identifier used to look the employee up.
     *
     * @return the employee's id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the employee's name as it appears on a payslip.
     *
     * @return the employee's name
     */
    public String getName() {
        return name;
    }
}
