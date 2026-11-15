package com.rosewood.payroll.employee;

import java.util.Optional;

/**
 * Where employee records are kept.
 *
 * <p>The payroll service depends on this interface rather than on a particular
 * store, so a real database can replace the in-memory version, and tests can supply
 * whatever employees or failures they need.
 */
public interface EmployeeRepository {

    /**
     * Looks up an employee by id.
     *
     * <p>An empty result means the store was read and holds no employee with this id.
     * If the store cannot be read, implementations must throw instead of returning
     * empty, so that a broken store is never mistaken for a missing employee.
     *
     * <p>A null id is a mistake in the calling code, not an id that is missing from the
     * store, so implementations must refuse it rather than report it as empty.
     *
     * @param id the employee's id
     * @return the employee, or an empty Optional if no employee has this id
     * @throws NullPointerException if id is null
     */
    Optional<Employee> findById(String id);
}
