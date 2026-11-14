package com.rosewood.payroll;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryEmployeeRepositoryTest {

    private final Employee efua = new Employee("e1", "Efua Sarpong");
    private final Employee kwame = new Employee("e2", "Kwame Owusu");

    @Test
    void findsEachEmployeeItWasGivenById() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(efua, kwame));

        assertSame(efua, repository.findById("e1"));
        assertSame(kwame, repository.findById("e2"));
    }

    // C10: the repository used to create e1 and e2 itself. It now holds only what it is given.
    @Test
    void holdsNoEmployeesItWasNotGiven() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of());

        assertNull(repository.findById("e1"));
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
        assertThrows(NullPointerException.class, () -> new InMemoryEmployeeRepository(null));
    }

    @Test
    void refusesAMissingEmployeeInTheList() {
        // List.of does not allow null, so Arrays.asList is used to build a list containing one.
        assertThrows(NullPointerException.class,
                () -> new InMemoryEmployeeRepository(Arrays.asList(efua, null)));
    }
}
