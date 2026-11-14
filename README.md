# Rosewood Payroll - Net Pay Service

This service works out an employee's payslip from the hours they worked and their
hourly rate: gross pay with overtime, the tax band, the tax, and net pay.

It started as a starter project written quickly with an AI assistant. That code
compiled and its three tests passed, but it paid unknown employees, accepted
negative hours, and turned every failure into a payslip of zero. This repository
contains the analysis of what was wrong, the refactored service, and a test suite
that checks what the code actually does.

- [ANALYSIS.md](ANALYSIS.md) explains what was wrong with the starter code and its
  tests, written before any code was changed.
- This README covers how to run the project, how it is built, and why it is built
  that way.
- The improvement beyond the brief is that tax bands that do not fit together are
  refused when the program starts. See [Improvement beyond the brief](#improvement-beyond-the-brief).

## Running it

The project compiles for Java 21 (`maven.compiler.release` in [pom.xml](pom.xml)).
The results in this README were produced with Maven 3.10.0 running on JDK 27. Every
command below is a plain Maven or JDK command, so it runs the same way on Windows,
macOS and Linux.

| Command | What it does | Result on the current code |
|---|---|---|
| `mvn clean verify` | Compiles, runs every test, and writes the JaCoCo coverage report to `target/site/jacoco/index.html` | 182 tests, 0 failures, `BUILD SUCCESS` |
| `mvn compile exec:java "-Dexec.mainClass=com.rosewood.payroll.app.Demo"` | Runs the demo, which prints two payslips and two refusal messages | The output shown below |
| `mvn test-compile org.pitest:pitest-maven:mutationCoverage` | Runs PIT mutation testing and writes the report to `target/pit-reports/index.html` | 80 mutants, 71 killed, test strength 99% (explained under [Tests](#tests)) |
| `mvn clean javadoc:javadoc -Ddoclint=all` | Builds the Javadoc with every lint check switched on, into `target/reports/apidocs/index.html` | 0 warnings |

The demo's `-D` argument is in quotes because Windows PowerShell otherwise splits it
at the first dot. The quoted form works in PowerShell, cmd, bash and zsh.

### Demo output

```
Payslip for Efua Sarpong (e1)
  Gross pay      GHS 800.00
  Tax band              LOW
  Tax             GHS 40.00
  Net pay        GHS 760.00
Payslip for Kwame Owusu (e2)
  Gross pay     GHS 1650.00
  Tax band           MEDIUM
  Tax            GHS 247.50
  Net pay       GHS 1402.50
Payslip for unknown refused: No employee found with id 'unknown'
Payslip for e1 refused: hoursWorked must be zero or more, but was -10
```

The last two lines are refusals and go to standard error. The starter demo
printed `0.0` for the unknown employee. Now the payslip is refused, with the reason.

## How the code is organised

The code is grouped by feature, so everything to do with tax lives in `tax`,
including the new tax band Rosewood is about to add.

| Package | Classes | Its one job |
|---|---|---|
| `com.rosewood.payroll` | [PayrollException](src/main/java/com/rosewood/payroll/PayrollException.java) | The base type for every payroll failure a caller is expected to handle |
| `employee` | [Employee](src/main/java/com/rosewood/payroll/employee/Employee.java), [EmployeeRepository](src/main/java/com/rosewood/payroll/employee/EmployeeRepository.java), [InMemoryEmployeeRepository](src/main/java/com/rosewood/payroll/employee/InMemoryEmployeeRepository.java), [EmployeeNotFoundException](src/main/java/com/rosewood/payroll/employee/EmployeeNotFoundException.java) | Finding employees |
| `pay` | [GrossPayCalculator](src/main/java/com/rosewood/payroll/pay/GrossPayCalculator.java), [Hours](src/main/java/com/rosewood/payroll/pay/Hours.java), [HourlyRate](src/main/java/com/rosewood/payroll/pay/HourlyRate.java), [InvalidPayInputException](src/main/java/com/rosewood/payroll/pay/InvalidPayInputException.java) | Checking the timesheet and working out gross pay, including overtime |
| `money` | [Money](src/main/java/com/rosewood/payroll/money/Money.java) | Holding an amount in cedis, rounded to whole pesewas in one place |
| `tax` | [TaxBand](src/main/java/com/rosewood/payroll/tax/TaxBand.java), [FlatRateTaxBand](src/main/java/com/rosewood/payroll/tax/FlatRateTaxBand.java), [TaxSchedule](src/main/java/com/rosewood/payroll/tax/TaxSchedule.java), [RosewoodTaxBands](src/main/java/com/rosewood/payroll/tax/RosewoodTaxBands.java), [InvalidTaxScheduleException](src/main/java/com/rosewood/payroll/tax/InvalidTaxScheduleException.java) | Deciding which band applies and how much tax it takes |
| `netpay` | [PayrollCalculator](src/main/java/com/rosewood/payroll/netpay/PayrollCalculator.java), [Payslip](src/main/java/com/rosewood/payroll/netpay/Payslip.java) | Putting the steps together into a payslip |
| `app` | [Demo](src/main/java/com/rosewood/payroll/app/Demo.java), [PayslipPrinter](src/main/java/com/rosewood/payroll/app/PayslipPrinter.java) | Wiring the parts together, and printing payslips and refusals |

Dependencies only point one way: `app` uses everything, `netpay` uses `employee`,
`pay`, `tax` and `money`, and `money` and the root package use nothing. I checked
this with `jdeps -verbose:package target/classes` after `mvn compile`, and there are
no cycles between packages.

## How a payslip is worked out

[PayrollCalculator.calculatePayslip](src/main/java/com/rosewood/payroll/netpay/PayrollCalculator.java)
goes through these steps, in this order:

1. **Check the timesheet and work out gross pay.** `GrossPayCalculator` refuses
   negative hours and a rate of zero or less, then pays every hour at the rate and
   adds half the rate again for each hour over 40. Gross pay is rounded to pesewas
   once, at the end.
2. **Find the employee.** If the id is not on record, no payslip is produced.
3. **Choose the tax band.** `TaxSchedule` picks the one band whose range includes
   the gross pay.
4. **Work out tax and net pay.** The band works out the tax, rounded to pesewas, and
   net pay is gross minus tax.
5. **Return a `Payslip`.** The payslip refuses to exist if tax is below zero or more
   than gross pay, or if net pay is not exactly gross minus tax.

Worked example, Kwame Owusu, 50 hours at 30: base pay 50 x 30 = 1500, plus overtime
premium 10 x 30 x 0.5 = 150, so gross is 1650.00. That falls in MEDIUM (1000 up to
5000, 15%), so tax is 247.50 and net pay is 1402.50.

| Band | Gross pay | Rate |
|---|---|---|
| LOW | 0 up to 1000 | 5% |
| MEDIUM | 1000 up to 5000 | 15% |
| HIGH | 5000 and above | 25% |

Each band includes its lower limit and stops just before its upper limit. The
whole gross is taxed at the band's rate.

## What was wrong

[ANALYSIS.md](ANALYSIS.md) has the full findings, each backed by something I ran.
The most serious was **C1**: a `catch (Exception e)` that returned `0` for any
failure. Because of it, an unknown employee got a payslip of `0.0`, which looks the
same as a person who worked no hours, and the real error was thrown away.

The fixes for C1 to C11 and T1 to T3 were made in small commits, and each commit
subject names the IDs it fixes, so `git log --oneline --grep=C6` shows the commit for C6.

| ID | Problem | What changed |
|---|---|---|
| C1 | Every failure became a payslip of 0 | The catch-all is gone. Failures reach the caller as exceptions |
| C2 | An unknown employee was paid | `findById` returns `Optional`, and a missing employee throws `EmployeeNotFoundException` |
| C3 | Negative hours and rates were accepted | `InvalidPayInputException` for negative hours or a rate of zero or less |
| C4 | Money held as `double`, never rounded | `BigDecimal` inside a `Money` type, rounded half-up to pesewas |
| C5 | One method did six jobs | Split into `GrossPayCalculator`, `TaxSchedule`, `TaxBand`, `Payslip` and `PayslipPrinter` |
| C6 | Tax bands in an `if / else` chain | `TaxBand` objects chosen by a `TaxSchedule` |
| C7 | The repository was hard-wired | Every dependency is passed in through the constructor |
| C8 | The calculation printed to the console | The calculator returns a `Payslip`, and `PayslipPrinter` prints it |
| C9 | Magic numbers | Named constants (`OVERTIME_THRESHOLD_HOURS`, `OVERTIME_PREMIUM`) and named bands in `RosewoodTaxBands` |
| C10 | Test employees inside the production repository | The repository is given its employees. `Demo` and the tests supply them |
| C11 | Hours and rate are easy to swap | Each has its own type, `Hours` and `HourlyRate`, so a call with the two swapped no longer compiles |
| C12 | The employee record is not used in the calculation | **Kept, as an assumption.** The brief does not say where hourly rates come from, so the rate stays an input to the calculation, as in the starter |
| T1 | A test mocked the class it was testing | The mock now stands in for the repository, and the real calculator runs |
| T2 | `assertNotNull` on a primitive | Asserts the exact net pay, 831.25 |
| T3 | Only checked net pay was positive | Asserts the exact net pay, 760.00 |
| T4 | Bands, overtime, edges and failures untested | Covered by the suite described under [Tests](#tests) |
| T5 | Coverage gave false comfort | PIT is used alongside JaCoCo, with before and after figures below |

## Key decisions

| Decision | Instead of | Why |
|---|---|---|
| Checked exceptions under an abstract `PayrollException` for an unknown employee and a bad timesheet | Unchecked exceptions | A correct caller can still hit these, because the data comes from outside. The compiler makes every caller decide what to do (Effective Java, Item 70). One `catch (PayrollException e)` handles every payroll failure without catching bugs |
| `NullPointerException` for null arguments | A custom exception | A null argument is a bug in the caller, not a payroll condition |
| An unchecked `InvalidTaxScheduleException` for bands that do not fit together | A checked exception | It is a set-up mistake found when the program starts, and nothing at run time can recover from it |
| `findById` returns `Optional<Employee>` | Returning `null`, or throwing from the repository | Not finding someone is a normal answer from a store. The calculator decides it is an error. An unreadable store must throw, and the interface says so |
| `BigDecimal` and a `Money` type | `double` | `double` cannot hold most decimal amounts exactly (`3.9425000000000003`). `Money` rounds in one place, so the rule cannot drift |
| Round half-up | Half-even (banker's rounding) | It is how a person checks a payslip by hand, so an employee working it out gets the same answer |
| Round gross once and tax separately, then net = gross - tax | Rounding net pay on its own | The three lines on a payslip always add up, and `Payslip` enforces it |
| `Payslip` refuses tax below zero or above gross pay | Trusting each band never to do that | Every band's result passes through `Payslip`. See [Improvement beyond the brief](#improvement-beyond-the-brief) |
| `Hours` and `HourlyRate` types that only refuse null | Types that also check their own value | A swap no longer compiles, and every timesheet rule stays in `GrossPayCalculator`, so a bad timesheet is still a checked `InvalidPayInputException` the caller must handle. `HourlyRate` is not `Money`, because a rate such as 124.975 must not be rounded |
| Check the timesheet before looking up the employee | Lookup first | A bad timesheet is reported as what it is, and the employee store is not touched. A test proves the store is never called |
| Reject a rate of zero, set no maximum for hours | Allowing zero, or inventing a cap | A zero rate is almost certainly a data-entry mistake. The pay period is not known, so any maximum for hours would be made up |
| Interfaces only for `EmployeeRepository` and `TaxBand` | An interface for every class | Those are the two places where a second implementation is expected. `GrossPayCalculator` is concrete but still injected |
| `TaxSchedule` alone decides which band covers an amount | Each band answering "do I apply?" itself | Bands that each answer for themselves can disagree with each other. With one place deciding, choosing a band and checking the bands use the same rule |
| Check the bands when the schedule is created | A fallback band for amounts nothing covers | A mistake is found when the program starts, not on payday. See [Improvement beyond the brief](#improvement-beyond-the-brief) |
| Feature packages | Layer packages (`model`, `service`, `repository`) | A change to tax stays in one package |
| Printing in `PayslipPrinter`, with its output streams passed in | Printing inside the calculator | The calculator only calculates, and tests can capture and check the exact printed text |
| Keep the original AI test file and fix it in place | Deleting it | The history shows each hollow test and how it was fixed |
| Write tests that confirm the defects before changing code | Fixing straight away | The findings in [ANALYSIS.md](ANALYSIS.md) are backed by tests anyone can rerun. The code as it was then is commit `a75d9dc` |

## Adding the new tax band

Rosewood is about to add a band with its own rules. To add it:

1. Write a class in `tax` that implements `TaxBand`: its name, where it starts, where
   it ends (or no end, if it is the top band), and `taxOn`, which returns the tax it
   takes from a gross amount. The rule can be anything that works from gross pay.
2. Add it to the list in `RosewoodTaxBands`, and set the upper limit of the band
   below it to where the new band starts.

Nothing else changes. `PayrollCalculator`, `TaxSchedule`, `Payslip` and
`PayslipPrinter` already work with any `TaxBand`.

[AddingATaxBandTest](src/test/java/com/rosewood/payroll/netpay/AddingATaxBandTest.java)
proves this. It adds a made-up VERY HIGH band, with a fixed 5000 plus 35% of pay
above 20000, a rule no existing band has. The band is written only inside the test.
The test shows that:

- the new band's rule works through the unchanged calculator: a gross of 30000.00
  gives tax 8500.00 and net pay 21500.00
- LOW, MEDIUM and HIGH produce exactly the same payslips as before
- forgetting to give HIGH an upper limit stops the schedule being created

The commit that added these changed no production class. The test also has a case
for a band that takes more than the gross pay, which is described under
[Improvement beyond the brief](#improvement-beyond-the-brief).

**The assumption behind this design:** a band is a range of gross pay, and "its own
rules" means how much tax it takes from that gross. If the new band depended on
something other than gross pay, such as the employee's status, the `TaxBand`
interface would have to change.

## Improvement beyond the brief

**Tax bands that do not fit together are refused when the program starts, and a band
that takes negative tax or more than the gross pay is refused on the first payslip.**

The brief asks that a new tax band can be added without rewriting existing code. The
`TaxBand` interface does that. The brief does not ask that a *wrong* set of bands be
refused, and that is where the risk is: adding a band means editing the list of
bands, and a typo in a limit does not crash anything. It pays someone the wrong
amount.

### What happens with one typo

Suppose MEDIUM is set to end at 4000 instead of 5000, while HIGH still starts at 5000:

| Version | Result |
|---|---|
| The starter (`else if (gross < 4000)`) | Nothing visible. Everyone earning 4000 to 4999 is taxed 25% instead of 15%, and no test notices |
| This design without the check | Nothing at startup. On payday, the first person earning 4000 to 4999 gets an error, after the run has started |
| This design with the check | The program does not start, and nobody is paid wrongly |

### What is checked

When a [TaxSchedule](src/main/java/com/rosewood/payroll/tax/TaxSchedule.java) is
created, which happens when the program starts, it checks that the bands fit
together. If they do not, it throws an `InvalidTaxScheduleException` that names the
bands and amounts involved:

| Mistake | Message, after `invalid tax schedule: ` |
|---|---|
| No bands | `there must be at least one band` |
| A band that does not end above where it starts | `band EMPTY must end above where it starts, but starts at GHS 1000.00 and ends at GHS 1000.00` |
| The lowest band does not start at 0 | `the lowest band, LOW, must start at GHS 0.00 but starts at GHS 100.00` |
| A gap between two bands | `there is a gap between band LOW, which ends at GHS 1000.00, and band HIGH, which starts at GHS 1200.00` |
| Two bands overlap | `band LOW, which ends at GHS 1200.00, overlaps band HIGH, which starts at GHS 1000.00` |
| A band below the top has no upper limit | `band HIGH has no upper limit, so band VERY HIGH can never apply` |
| The top band has an upper limit | `the highest band, HIGH, must have no upper limit, but ends at GHS 9000.00` |

The startup check sees each band's range, not the tax its rule takes. `Payslip` adds
a second check: tax below zero or above gross pay is refused, so a band that does
that fails on the first payslip it touches. A wrong rate that stays within those
limits is not caught by either check; that is what each band's own tests are for.

The exception is unchecked because it is a set-up mistake. Nothing can recover from
it while the program runs. The only fix is to correct the bands.

### Proof

- [TaxScheduleTest](src/test/java/com/rosewood/payroll/tax/TaxScheduleTest.java)
  has a test for each mistake in the table, asserting the exact message, and checks
  that a single band covering everything is accepted. PIT kills all 24 mutants in
  `TaxSchedule`.
- [AddingATaxBandTest](src/test/java/com/rosewood/payroll/netpay/AddingATaxBandTest.java)
  covers the likeliest real mistake, forgetting to cap HIGH when VERY HIGH is added,
  and a band whose rule takes more than the gross pay.
- [PayslipTest](src/test/java/com/rosewood/payroll/netpay/PayslipTest.java) covers
  the tax check on the payslip.

### Seeing it happen

1. In [RosewoodTaxBands.java](src/main/java/com/rosewood/payroll/tax/RosewoodTaxBands.java),
   change MEDIUM's upper limit from `money("5000")` to `money("4000")`.
2. Run the demo:
   `mvn compile exec:java "-Dexec.mainClass=com.rosewood.payroll.app.Demo"`.
   No payslip is printed, and the run stops with:
   `invalid tax schedule: there is a gap between band MEDIUM, which ends at GHS 4000.00, and band HIGH, which starts at GHS 5000.00`
3. Undo the change:
   `git restore src/main/java/com/rosewood/payroll/tax/RosewoodTaxBands.java`

## Failures and exceptions

| Exception | Kind | Thrown when | What it carries |
|---|---|---|---|
| `EmployeeNotFoundException` | Checked, a `PayrollException` | The employee id is not on record | `getEmployeeId()` |
| `InvalidPayInputException` | Checked, a `PayrollException` | Hours are negative, or the rate is zero or less | `getField()`, `getRejectedValue()` |
| `InvalidTaxScheduleException` | Unchecked, an `IllegalArgumentException` | A band does not end above where it starts, or the bands do not start at zero, leave a gap, overlap, or do not end with one open-ended band | A message naming the bands involved |
| `NullPointerException` | Unchecked | Any required argument is null | A message naming the argument, such as `employeeId must not be null`. A null band inside the list given to `TaxSchedule` is reported by `List.copyOf`, without a message |
| `IllegalArgumentException` | Unchecked | A payslip whose tax is negative or more than gross pay, or whose lines do not add up; a duplicate employee id; or a badly set up band | A message with the values involved |
| `IllegalStateException` | Unchecked | `TaxSchedule.bandFor` is given an amount no band covers | The amount. A checked schedule covers everything from zero up, so only a negative amount, which gross pay never is, can cause it |

Nothing catches `Exception`, and nothing is swallowed. The only `catch` in the
production code is in `Demo`, at the edge of the program. It catches
`PayrollException`, prints the refusal and its reason to standard error, and goes on
to the next payslip. Anything else, which would be a bug, is left to stop the
program.

## Tests

The suite uses JUnit 5 and Mockito. Each expected amount was worked out by hand
first, so the tests check that results are right, not just that code ran.

| Test class | Tests | What it covers |
|---|---|---|
| [PayrollCalculatorTest](src/test/java/com/rosewood/payroll/netpay/PayrollCalculatorTest.java) | 3 | The original AI tests, fixed in place (T1 to T3) |
| [PayrollCalculatorBehaviourTest](src/test/java/com/rosewood/payroll/netpay/PayrollCalculatorBehaviourTest.java) | 26 | Every band and both sides of every band edge, overtime, zero hours, the full payslip, unknown and missing employees, a failing repository, invalid input, and rounding |
| [AddingATaxBandTest](src/test/java/com/rosewood/payroll/netpay/AddingATaxBandTest.java) | 6 | A new band with its own rule plugs in without changing existing classes, and a band that takes more than the gross pay is refused |
| [PayRulesThatAlwaysHoldTest](src/test/java/com/rosewood/payroll/netpay/PayRulesThatAlwaysHoldTest.java) | 72 | Rules that must hold for every timesheet, checked on a grid of 8 hours values x 8 rates: tax between zero and gross pay, net pay never negative, and more hours never less gross pay |
| [PayslipTest](src/test/java/com/rosewood/payroll/netpay/PayslipTest.java) | 6 | A payslip whose tax is below zero or above gross pay, or whose lines do not add up, is refused, and both edges are accepted |
| [GrossPayCalculatorTest](src/test/java/com/rosewood/payroll/pay/GrossPayCalculatorTest.java) | 10 | Overtime, rounding gross once, and timesheet checks |
| [HoursTest](src/test/java/com/rosewood/payroll/pay/HoursTest.java) | 2 | Part hours kept exactly, and a missing value refused |
| [HourlyRateTest](src/test/java/com/rosewood/payroll/pay/HourlyRateTest.java) | 2 | A rate kept without rounding, and a missing value refused |
| [TaxScheduleTest](src/test/java/com/rosewood/payroll/tax/TaxScheduleTest.java) | 19 | Choosing a band at every edge, and every way a set of bands can fail to fit together |
| [FlatRateTaxBandTest](src/test/java/com/rosewood/payroll/tax/FlatRateTaxBandTest.java) | 9 | Ranges, the whole-gross rule, rounding, and refusing bad rates and bounds |
| [MoneyTest](src/test/java/com/rosewood/payroll/money/MoneyTest.java) | 13 | Half-up rounding, equality, arithmetic and formatting |
| [EmployeeTest](src/test/java/com/rosewood/payroll/employee/EmployeeTest.java) | 5 | Keeping the id and name, and refusing null, empty or whitespace-only values for either field |
| [InMemoryEmployeeRepositoryTest](src/test/java/com/rosewood/payroll/employee/InMemoryEmployeeRepositoryTest.java) | 6 | Finding employees, and refusing duplicate ids and nulls |
| [PayslipPrinterTest](src/test/java/com/rosewood/payroll/app/PayslipPrinterTest.java) | 3 | The exact printed payslip, and refusals going to their own stream |

Mockito is used where a test needs the repository to behave in a way the real one
cannot, for example to throw, or to prove it was never called. The calculator under
test is always the real one.

Most tests are worked examples. `PayRulesThatAlwaysHoldTest` checks rules instead, on
a fixed grid: every hours value in a short list combined with every rate in another,
written out in the test so every run uses the same inputs. The values sit on the
overtime threshold and on each band edge. One rule is left out on purpose, "more gross
pay never means less net pay", because the tax cliff makes it false (see
[Kept on purpose](#kept-on-purpose)).

PIT makes no mutants for the checks inside the `Payslip` record. Each one was checked
by switching it off in a throwaway copy and confirming that its test fails.

### Before and after

Both columns measure the whole project. The starter's figures for `PayrollCalculator`
alone, which [ANALYSIS.md](ANALYSIS.md) uses, are shown in brackets.

| Measure | Starter | Now |
|---|---|---|
| Tests | 3 | 182 |
| JaCoCo line coverage | 21 of 34 lines (10 of 16 in `PayrollCalculator`) | 194 of 210 lines, and all 16 missed lines are in `Demo` |
| JaCoCo branch coverage | 2 of 6 | 56 of 56 |
| PIT mutants killed | 3 of 21 (2 of 15 in `PayrollCalculator`) | 71 of 80 |

Of the 9 mutants PIT does not kill:

- **8 are in `Demo`**, which has no tests. It only wires the parts together, and
  running it shows its output.
- **1 is equivalent.** Changing `hoursWorked > 40` to `>= 40` gives the same pay,
  because at exactly 40 hours the overtime premium is 0 either way. No test can tell
  the two versions apart.

That is what PIT's test strength of 99% means: of the mutants the tests run, every
one that changes behaviour is caught.

## The AI's code and tests: kept, fixed, rewritten

| Part | Outcome | Notes |
|---|---|---|
| The pay rules: 40-hour threshold, time-and-a-half, bands at 1000 and 5000 with rates of 5%, 15% and 25% | Kept | Pinned by tests before any change, so the refactor could not alter them |
| `Employee` | Fixed | Moved to `employee`, documented, and refuses null or blank ids and names |
| `EmployeeRepository` | Fixed | Returns `Optional` instead of `null`, and documents what an empty result means |
| `InMemoryEmployeeRepository` | Fixed | Given its employees instead of creating them, and refuses duplicate ids |
| `PayrollCalculator` | Rewritten | Now coordinates injected parts and returns a `Payslip`, with no catch-all and no printing |
| `Demo` | Rewritten | Wires the parts together and handles refusals at the edge |
| `PayrollCalculatorTest` | Fixed | All three tests kept under their original names, each now checking an exact amount |

## Behaviour changes

These are deliberate changes from what the starter code did, each covered by a test.

| Input | Starter | Now |
|---|---|---|
| Unknown employee | `0.0` | `EmployeeNotFoundException` |
| `null` employee id | `0.0` | `NullPointerException` |
| -10 hours at 20 | `-190.0` | `InvalidPayInputException` |
| 40 hours at -20 | `-760.0` | `InvalidPayInputException` |
| 40 hours at 0 | `0.0` | `InvalidPayInputException` |
| `NaN` or infinite hours | `NaN` | Cannot be passed, because `BigDecimal` has no such values |
| 41 hours at 0.10 | `3.9425000000000003` | `3.94` |
| 37.5 hours at 13.33 | `474.88125` | `474.89` |
| Return value | A bare `double` | A `Payslip` with gross pay, band, tax and net pay |
| Printing | Inside the calculation | In `PayslipPrinter`, in a new format that shows every line |

### Kept on purpose

**The whole gross is taxed at one rate, which leaves a cliff at each band edge.** A
gross of 999.99 nets 949.99, but a gross of 1000.00 nets only 850.00. That is how the
starter code taxed pay, and changing it to marginal tax would change people's pay
without being asked. Tests pin both sides of each edge, so the behaviour is clear and
easy to change if Rosewood decides it should be.
