package com.rosewood.payroll.employee;

import java.util.Optional;

public interface EmployeeRepository {

    /**
     * Looks up an employee by id.
     *
     * @return the employee, or an empty Optional if no employee has this id
     */
    Optional<Employee> findById(String id);
}
