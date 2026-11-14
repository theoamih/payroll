package com.rosewood.payroll.pay;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The pay for one ordinary hour of work, in cedis.
 *
 * <p>Like {@link Hours}, it has its own type so the two cannot be swapped in a call.
 *
 * <p>It is not a {@code Money}, because {@code Money} rounds to whole pesewas and a
 * rate must not be rounded: a rate such as 124.975 is real, and only the pay worked
 * out from it is rounded. This type does not check the value either;
 * {@link GrossPayCalculator} does.
 *
 * @param value the rate in cedis per hour, at any precision
 */
public record HourlyRate(BigDecimal value) {

    /**
     * Creates an hourly rate.
     *
     * @param value the rate in cedis per hour, at any precision
     * @throws NullPointerException if value is null
     */
    public HourlyRate {
        Objects.requireNonNull(value, "hourly rate value must not be null");
    }
}
