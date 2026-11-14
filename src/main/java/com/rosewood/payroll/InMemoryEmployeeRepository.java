package com.rosewood.payroll;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<String, Employee> employees = new HashMap<>();

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
    public Employee findById(String id) {
        return employees.get(id); // returns null when the id is not found
    }
}
