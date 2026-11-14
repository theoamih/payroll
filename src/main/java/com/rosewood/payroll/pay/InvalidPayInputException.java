package com.rosewood.payroll.pay;

import com.rosewood.payroll.PayrollException;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Thrown when the hours worked or the hourly rate cannot produce a valid payslip,
 * for example negative hours from a mistyped timesheet.
 *
 * <p>It is a checked {@link PayrollException} because these values come from
 * timesheet data, so a correct caller can still receive bad ones and must decide what
 * to do about them.
 */
public class InvalidPayInputException extends PayrollException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The name of the input that was rejected. */
    private final String field;

    /** The value that was rejected. */
    private final BigDecimal rejectedValue;

    /**
     * Creates the exception for one rejected input.
     *
     * @param field         the name of the input that was rejected
     * @param rejectedValue the value that was rejected
     * @param requirement   what a valid value must be, completing the sentence
     *                      "{field} must be ..."
     */
    public InvalidPayInputException(String field, BigDecimal rejectedValue, String requirement) {
        super(field + " must be " + requirement + ", but was " + rejectedValue.toPlainString());
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    /**
     * Gives the rejected input's name as its own value, so callers can point to the
     * wrong field without having to pick it out of the message.
     *
     * @return the name of the input that was rejected
     */
    public String getField() {
        return field;
    }

    /**
     * Returns the value that was rejected.
     *
     * @return the rejected value
     */
    public BigDecimal getRejectedValue() {
        return rejectedValue;
    }
}
