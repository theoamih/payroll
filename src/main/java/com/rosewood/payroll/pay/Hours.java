package com.rosewood.payroll.pay;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The hours an employee worked in one pay period.
 *
 * <p>Hours and the hourly rate are both decimal numbers. Giving each its own type
 * means a call with the two swapped no longer compiles, instead of quietly paying
 * the wrong amount.
 *
 * <p>This type does not check the value. {@link GrossPayCalculator} does, so every
 * timesheet rule is in one place and a bad timesheet is reported as an
 * {@link InvalidPayInputException} that the caller must handle.
 *
 * @param value the number of hours, at any precision
 */
public record Hours(BigDecimal value) {

    /**
     * Creates the hours worked.
     *
     * @param value the number of hours, at any precision
     * @throws NullPointerException if value is null
     */
    public Hours {
        Objects.requireNonNull(value, "hours value must not be null");
    }
}
