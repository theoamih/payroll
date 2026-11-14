package com.rosewood.payroll.pay;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HoursTest {

    // Part hours are real, for example 37.5, so the value is kept exactly as given.
    @Test
    void keepsTheHoursExactlyAsGiven() {
        Hours hours = new Hours(new BigDecimal("37.5"));

        assertEquals(new BigDecimal("37.5"), hours.value());
    }

    @Test
    void refusesAMissingValue() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> new Hours(null));

        assertEquals("hours value must not be null", error.getMessage());
    }
}
