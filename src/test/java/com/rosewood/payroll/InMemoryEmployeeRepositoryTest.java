package com.rosewood.payroll;

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

    // C2: an unknown id is reported as an empty Optional, not null.
    @Test
    void reportsAnUnknownIdAsEmpty() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(efua));

        assertEquals(Optional.empty(), repository.findById("unknown"));
    }

    // C10: the repository used to create e1 and e2 itself. It now holds only what it is given.
    @Test
    void holdsNoEmployeesItWasNotGiven() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of());

        assertEquals(Optional.empty(), repository.findById("e1"));
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
