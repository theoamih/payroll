package com.rosewood.payroll.employee;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryEmployeeRepositoryTest {

    private final Employee efua = new Employee("e1", "Efua Sarpong");

    private final Employee kwame = new Employee("e2", "Kwame Owusu");

    @Test
    void findsEachEmployeeItWasGivenById() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(efua, kwame));

        assertSame(efua, repository.findById("e1").orElseThrow());
        assertSame(kwame, repository.findById("e2").orElseThrow());
    }

    // C2: an empty Optional makes "not found" part of the method's type, so a caller
    // cannot forget to handle it the way the starter code forgot to check for null.
    @Test
    void reportsAnUnknownIdAsEmpty() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(efua));

        assertEquals(Optional.empty(), repository.findById("unknown"));
    }

    // C10: a repository that quietly contains employees of its own lets tests pass
    // because of data they cannot see. It must hold only what it is given.
    @Test
    void holdsNoEmployeesItWasNotGiven() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of());

        assertEquals(Optional.empty(), repository.findById("e1"));
    }

    // A null id is a mistake in the calling code. Reporting it as "not found" would hide
    // that mistake, so it is refused instead.
    @Test
    void refusesToLookUpANullId() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(efua));

        NullPointerException error = assertThrows(NullPointerException.class, () -> repository.findById(null));

        assertEquals("id must not be null", error.getMessage());
    }

    @Test
    void refusesTwoEmployeesWithTheSameId() {
        Employee anotherE1 = new Employee("e1", "Someone Else");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new InMemoryEmployeeRepository(List.of(efua, anotherE1)));

        assertEquals("duplicate employee id: e1", error.getMessage());
    }

    @Test
    void refusesAMissingListOfEmployees() {
        NullPointerException error = assertThrows(NullPointerException.class,
                () -> new InMemoryEmployeeRepository(null));

        assertEquals("employees must not be null", error.getMessage());
    }

    @Test
    void refusesAMissingEmployeeInTheList() {
        // List.of does not allow null, so Arrays.asList is used to build a list containing one.
        List<Employee> employeesWithANull = Arrays.asList(efua, null);

        NullPointerException error = assertThrows(NullPointerException.class,
                () -> new InMemoryEmployeeRepository(employeesWithANull));

        assertEquals("employees must not contain null", error.getMessage());
    }
}
