package com.rosewood.payroll.money;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    // Halves round up, the way a person rounds by hand. The amounts are built from
    // strings, so 1.005 is exactly 1.005 and not the nearest double to it.
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "1.004,  1.00",
            "1.005,  1.01",
            "1.015,  1.02",
            "0.125,  0.13",
            "760,    760.00",
            "-1.005, -1.01",
    })
    void roundsToWholePesewasWithHalvesRoundingUp(BigDecimal amount, String expected) {
        Money money = new Money(amount);

        assertEquals(new BigDecimal(expected), money.amount());
    }

    // Every amount is held to two decimal places, so equal amounts are equal however
    // they were written.
    @Test
    void treatsTheSameAmountAsEqualWhateverItsOriginalPrecision() {
        Money writtenAsWhole = new Money(new BigDecimal("5"));
        Money writtenInPesewas = new Money(new BigDecimal("5.00"));

        assertEquals(writtenAsWhole, writtenInPesewas);
        assertEquals(writtenAsWhole.hashCode(), writtenInPesewas.hashCode());
    }

    @Test
    void multipliesAndRoundsTheResultToPesewas() {
        Money gross = new Money(new BigDecimal("0.10"));

        Money tax = gross.times(new BigDecimal("0.05"));

        // 0.10 x 0.05 = 0.005, exactly half a pesewa, which rounds up
        assertEquals(new Money(new BigDecimal("0.01")), tax);
    }

    @Test
    void subtractsOneAmountFromAnother() {
        Money gross = new Money(new BigDecimal("1650.00"));
        Money tax = new Money(new BigDecimal("247.50"));

        assertEquals(new Money(new BigDecimal("1402.50")), gross.minus(tax));
    }

    @Test
    void comparesAmountsByValue() {
        Money smaller = new Money(new BigDecimal("999.99"));
        Money larger = new Money(new BigDecimal("1000"));

        assertTrue(smaller.isLessThan(larger));
        assertFalse(larger.isLessThan(smaller));
        assertFalse(larger.isLessThan(new Money(new BigDecimal("1000.00"))));
    }

    @Test
    void showsTheAmountAsItWouldAppearOnAPayslip() {
        assertEquals("GHS 760.00", new Money(new BigDecimal("760")).toString());
    }

    @Test
    void refusesAMissingAmount() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }
}
