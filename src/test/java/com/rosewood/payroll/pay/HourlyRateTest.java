package com.rosewood.payroll.pay;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HourlyRateTest {

    // A rate is not rounded to pesewas. Only the pay worked out from it is, so a rate
    // of 124.975 must still be 124.975 here.
    @Test
    void keepsTheRateWithoutRoundingIt() {
        HourlyRate rate = new HourlyRate(new BigDecimal("124.975"));

        assertEquals(new BigDecimal("124.975"), rate.value());
    }

    @Test
    void refusesAMissingValue() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> new HourlyRate(null));

        assertEquals("hourly rate value must not be null", error.getMessage());
    }
}
