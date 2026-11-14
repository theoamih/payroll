package com.rosewood.payroll.netpay;

import com.rosewood.payroll.PayrollException;
import com.rosewood.payroll.employee.Employee;
import com.rosewood.payroll.employee.InMemoryEmployeeRepository;
import com.rosewood.payroll.money.Money;
import com.rosewood.payroll.pay.GrossPayCalculator;
import com.rosewood.payroll.tax.FlatRateTaxBand;
import com.rosewood.payroll.tax.InvalidTaxScheduleException;
import com.rosewood.payroll.tax.RosewoodTaxBands;
import com.rosewood.payroll.tax.TaxBand;
import com.rosewood.payroll.tax.TaxSchedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Shows that a new tax band with its own rule can be added without changing any
 * existing class.
 *
 * <p>Rosewood is about to add a band with rules the current bands do not have. This
 * test adds a made-up one, {@link FixedPlusPercentageTaxBand}, written only here, and
 * runs it through the real calculator. Nothing in the production code knows it exists.
 */
class AddingATaxBandTest {

    private static final String EMPLOYEE = "e1";

    private static final BigDecimal FORTY_HOURS = new BigDecimal("40");

    private static final TaxBand VERY_HIGH = new FixedPlusPercentageTaxBand(
            "VERY HIGH", money("20000"), money("5000"), new BigDecimal("0.35"));

    // Adding a band on top means HIGH now stops where VERY HIGH starts. That is a change
    // to one number in the list of bands, not to any code that uses the bands.
    private static final TaxBand HIGH_UP_TO_TWENTY_THOUSAND = FlatRateTaxBand.between(
            "HIGH", money("5000"), money("20000"), new BigDecimal("0.25"));

    private final InMemoryEmployeeRepository repository =
            new InMemoryEmployeeRepository(List.of(new Employee(EMPLOYEE, "Efua Sarpong")));

    /**
     * A band with a rule no existing band has: a fixed amount plus a percentage of the
     * pay above the point where the band starts. It is open-ended, so it is the highest band.
     */
    private static final class FixedPlusPercentageTaxBand implements TaxBand {

        private final String name;

        private final Money start;

        private final Money fixedAmount;

        private final BigDecimal rateAboveStart;

        private FixedPlusPercentageTaxBand(String name, Money start, Money fixedAmount, BigDecimal rateAboveStart) {
            this.name = name;
            this.start = start;
            this.fixedAmount = fixedAmount;
            this.rateAboveStart = rateAboveStart;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Money lowerBound() {
            return start;
        }

        @Override
        public Optional<Money> upperBound() {
            return Optional.empty();
        }

        @Override
        public Money taxOn(Money grossPay) {
            Money payAboveStart = grossPay.minus(start);
            Money percentagePart = payAboveStart.times(rateAboveStart);

            return new Money(fixedAmount.amount().add(percentagePart.amount()));
        }
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }

    private PayrollCalculator calculatorWith(TaxSchedule schedule) {
        return new PayrollCalculator(repository, new GrossPayCalculator(), schedule);
    }

    // 40h at 750 is a gross of 30000. VERY HIGH takes 5000 plus 35% of the 10000 above
    // 20000, which is 3500, so the tax is 8500 and net pay is 21500.
    @Test
    void appliesTheNewBandsOwnRuleThroughTheUnchangedCalculator() throws PayrollException {
        TaxSchedule scheduleWithNewBand = new TaxSchedule(List.of(
                RosewoodTaxBands.LOW, RosewoodTaxBands.MEDIUM, HIGH_UP_TO_TWENTY_THOUSAND, VERY_HIGH));

        Payslip payslip = calculatorWith(scheduleWithNewBand)
                .calculatePayslip(EMPLOYEE, FORTY_HOURS, new BigDecimal("750"));

        assertEquals(money("30000.00"), payslip.grossPay());
        assertEquals("VERY HIGH", payslip.taxBand());
        assertEquals(money("8500.00"), payslip.tax());
        assertEquals(money("21500.00"), payslip.netPay());
    }

    // The existing bands keep producing exactly the payslips they produced before the
    // new band was added.
    @ParameterizedTest(name = "{0}h at {1} -> {2} band, net {3}")
    @CsvSource({
            "40, 20,  LOW,    760.00",
            "40, 25,  MEDIUM, 850.00",
            "40, 200, HIGH,   6000.00",
    })
    void leavesThePayslipsOfTheExistingBandsUnchanged(BigDecimal hours, BigDecimal rate,
                                                     String expectedBand, String expectedNet)
            throws PayrollException {

        TaxSchedule scheduleWithNewBand = new TaxSchedule(List.of(
                RosewoodTaxBands.LOW, RosewoodTaxBands.MEDIUM, HIGH_UP_TO_TWENTY_THOUSAND, VERY_HIGH));

        Payslip payslip = calculatorWith(scheduleWithNewBand).calculatePayslip(EMPLOYEE, hours, rate);

        assertEquals(expectedBand, payslip.taxBand());
        assertEquals(money(expectedNet), payslip.netPay());
    }

    // The likely mistake when adding the band is to forget to give HIGH an upper limit.
    // HIGH would then still cover everything from 5000 up and VERY HIGH would never
    // apply. The schedule refuses to be created, so this is found at startup, not by an
    // employee whose payslip is wrong.
    @Test
    void refusesToStartIfTheBandBelowWasNotGivenAnUpperLimit() {
        List<TaxBand> bandsWithHighStillOpenEnded = List.of(
                RosewoodTaxBands.LOW, RosewoodTaxBands.MEDIUM, RosewoodTaxBands.HIGH, VERY_HIGH);

        InvalidTaxScheduleException error = assertThrows(InvalidTaxScheduleException.class,
                () -> new TaxSchedule(bandsWithHighStillOpenEnded));

        assertEquals("invalid tax schedule: band HIGH has no upper limit, so band VERY HIGH can never apply",
                error.getMessage());
    }
}
