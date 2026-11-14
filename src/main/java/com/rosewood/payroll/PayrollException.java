package com.rosewood.payroll;

import java.io.Serial;

/**
 * Base class for every failure the payroll service reports on purpose, so a caller
 * can handle all of them in one place without also catching programming errors.
 *
 * It is checked, so the compiler makes every caller deal with these failures rather
 * than letting one pass unnoticed. It is abstract so that each failure has to be
 * reported as a specific type that says what went wrong.
 */
public abstract class PayrollException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    protected PayrollException(String message) {
        super(message);
    }
}
