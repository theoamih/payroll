package com.rosewood.payroll.employee;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An employee store held in memory, for the demo and for tests.
 *
 * <p>Its contents are fixed when it is created. It holds only the employees it is
 * given, so no test or demo depends on employees it cannot see.
 */
public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<String, Employee> employees = new HashMap<>();

    /**
     * Creates a store holding exactly the given employees.
     *
     * @param employees the employees to hold
     * @throws NullPointerException     if the collection, or any employee in it, is null
     * @throws IllegalArgumentException if two employees share an id
     */
    public InMemoryEmployeeRepository(Collection<Employee> employees) {
        Objects.requireNonNull(employees, "employees must not be null");

        for (Employee employee : employees) {
            Objects.requireNonNull(employee, "employees must not contain null");

            // Two records with one id would silently replace each other, so one person
            // could be paid under another's record. Refuse it here instead.
            if (this.employees.putIfAbsent(employee.getId(), employee) != null) {
                throw new IllegalArgumentException("duplicate employee id: " + employee.getId());
            }
        }
    }

    @Override
    public Optional<Employee> findById(String id) {
        Objects.requireNonNull(id, "id must not be null");

        return Optional.ofNullable(employees.get(id));
    }
}
