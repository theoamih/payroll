package com.rosewood.payroll.employee;

import java.util.Objects;

/**
 * A person on the payroll.
 *
 * <p>The id and name are checked when the record is created. The name is printed on
 * every payslip and the id is how the employee is found, so a record without either
 * would produce a payslip such as "Payslip for null (e1)" instead of failing where the
 * bad record was made.
 */
public class Employee {

    private final String id;

    private final String name;

    /**
     * Creates an employee record.
     *
     * @param id   the identifier used to look the employee up
     * @param name the employee's name as it appears on a payslip
     * @throws NullPointerException     if id or name is null
     * @throws IllegalArgumentException if id or name is blank
     */
    public Employee(String id, String name) {
        this.id = requireNotBlank(id, "id");
        this.name = requireNotBlank(name, "name");
    }

    // Null is a mistake in the calling code, reported the same way as everywhere else
    // in the project. Blank is refused too, because a blank name prints an empty
    // payslip heading and a blank id cannot sensibly be looked up.
    private static String requireNotBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value;
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
