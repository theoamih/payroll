package com.rosewood.payroll.employee;

/**
 * A person on the payroll.
 */
public class Employee {

    private final String id;

    private final String name;

    /**
     * @param id   the identifier used to look the employee up
     * @param name the employee's name as it appears on a payslip
     */
    public Employee(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return the identifier used to look the employee up
     */
    public String getId() {
        return id;
    }

    /**
     * @return the employee's name as it appears on a payslip
     */
    public String getName() {
        return name;
    }
}
