package com.rosewood.payroll.tax;

import java.io.Serial;

/**
 * Thrown when a tax schedule is set up with bands that would leave some gross pay
 * with no band, or with more than one band.
 *
 * <p>It is thrown when the schedule is created, so a badly set-up band is found when
 * the program starts rather than on payday, when someone's pay falls into the gap.
 *
 * <p>It is unchecked, unlike {@link com.rosewood.payroll.PayrollException}. A broken
 * schedule is a mistake in how the program was set up, not something a correct caller
 * can meet while running payroll, so there is nothing for a caller to recover from.
 * It extends {@link IllegalArgumentException} because the bands passed in are wrong.
 */
public class InvalidTaxScheduleException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a description of what is wrong with the bands.
     *
     * @param problem what is wrong, for example a gap between two named bands
     */
    public InvalidTaxScheduleException(String problem) {
        super("invalid tax schedule: " + problem);
    }
}
