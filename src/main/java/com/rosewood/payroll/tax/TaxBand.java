package com.rosewood.payroll.tax;

import com.rosewood.payroll.money.Money;

import java.util.Optional;

/**
 * One band of the tax schedule: a range of gross pay and the rule for how much tax is
 * taken from gross pay in that range.
 *
 * <p>A band only declares its range. Which band applies to a gross amount is decided
 * in one place, {@link TaxSchedule}, from these ranges, so the band that is chosen can
 * never disagree with the range a band declares.
 *
 * <p>How much tax a band takes is entirely its own rule. A new kind of band, with a
 * rule the existing bands do not have, is added by writing a new implementation of
 * this interface. None of the existing bands, and nothing that uses them, has to change.
 *
 * <p>Implementations must be immutable: a band's name, bounds and rule must not change
 * after it is created. {@link TaxSchedule} checks that the bands fit together only
 * once, when it is created, so a band that changed its bounds afterwards could open a
 * gap or an overlap that nothing would detect.
 */
public interface TaxBand {

    /**
     * Returns the name shown on a payslip for this band.
     *
     * @return the band's name, for example "LOW"
     */
    String name();

    /**
     * Returns the smallest gross pay this band covers. The band includes this amount.
     *
     * @return the lower bound of the band, inclusive
     */
    Money lowerBound();

    /**
     * Returns the gross pay at which the next band starts. The band does not include
     * this amount.
     *
     * @return the upper bound of the band, exclusive, or empty if the band has no
     *         upper limit
     */
    Optional<Money> upperBound();

    /**
     * Works out the tax this band takes from a gross amount within its range.
     *
     * @param grossPay gross pay that falls within this band's range
     * @return the tax to take, in whole pesewas
     */
    Money taxOn(Money grossPay);
}
