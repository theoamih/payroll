# What's wrong with the net-pay service

I wrote this before changing any code. I didn't want to trust anything I had only
read, so every claim below comes from something I ran: the demo, the existing
tests, JaCoCo, PIT, or a direct call to `calculateNetPay` with the inputs shown.

## First impressions vs. what the tools said

On the surface the code looks finished:

- The demo prints sensible numbers: `760.0` for e1, `1402.5` for e2.
- All three tests pass.
- JaCoCo says 10 of the 16 lines in `PayrollCalculator` are covered.

Mutation testing tells a different story. PIT made 15 small deliberate changes to
`PayrollCalculator`, such as turning `>` into `>=`, `+` into `-`, or dropping the
tax subtraction, and reran the tests after each one. The tests failed for 2 of the
15. The other 13 broke the pay calculation and nothing noticed.

The demo also prints `0.0` for an employee who doesn't exist, which the README
asks you to look at. That turned out to be the most important clue.

To reproduce:

```bash
mvn compile exec:java -Dexec.mainClass=com.rosewood.payroll.Demo
mvn verify                                                   # tests + JaCoCo
mvn test-compile org.pitest:pitest-maven:mutationCoverage    # PIT
```

---

## Part 1 - Problems in the code

These are in order of how much damage they could do to someone's payslip.

### C1. Every failure becomes a payslip of 0 (the most serious problem)

```java
} catch (Exception e) {
    // something went wrong (for example the employee was not found) - return zero
    return 0;
}
```

Whatever goes wrong, the caller gets `0.0` back. That covers an unknown employee,
a `null` id, and any bug in the code. But `0.0` is also the right answer for
someone who worked no hours that week, so the caller has no way to tell "this
person earned nothing" from "the system broke". The exception itself is thrown
away: no message, no cause, nothing logged.

I rank this first because of what it means in payroll. If a payroll run stops
with an error, someone gets paid a day late. If it quietly produces the wrong
amount, someone loses wages and nobody finds out until they complain. This
`catch` turns every failure into the quiet kind, and it's also what kept the next
problem hidden.

### C2. Nobody checks whether the employee exists

```java
// EmployeeRepository
Employee findById(String id);

// InMemoryEmployeeRepository
return employees.get(id); // returns null when the id is not found
```

`findById` returns `null` when it can't find someone. The method signature doesn't
say so; only a comment in a different file does. `calculateNetPay` never checks.
It works out the full pay for a person who doesn't exist, then crashes on
`employee.getName()` inside the `println`, and the `catch` from C1 turns the crash
into `0`.

I wanted to be sure, so I made a copy of the code, deleted only the `println`
line, and ran the demo again:

```
Result unknown employee -> 760.0
```

The only thing stopping the system from paying made-up employee IDs is a logging
line. Someone tidying up console output would remove that protection without
knowing it was there, and all three tests would still pass.

### C3. Nothing stops impossible inputs

Here's what employee e1 gets paid:

| Hours | Rate | Result |
|---|---|---|
| -10 | 20 | `-190.0` |
| 40 | -20 | `-760.0` |
| `NaN` | 20 | `NaN` |
| `Infinity` | 20 | `NaN` |

`NaN` even lands in the 25% band, because the last `else` takes anything that
fails the two comparisons above it. The brief says it's unclear what happens when
the hours are invalid. The answer is that the code pays a negative or meaningless
amount and prints it like a normal payslip.

### C4. Money is stored as `double` and never rounded

| Hours | Rate | Result |
|---|---|---|
| 41 | 0.10 | `3.9425000000000003` |
| 37.5 | 13.33 | `474.88125` |
| 40 | 24.99975 | `949.9905` |

A payslip shows cedis and pesewas, i.e. two decimal places. `double` can't hold
most decimal amounts exactly, and nothing in the code rounds the result, so gross
pay, tax and net pay aren't guaranteed to be whole pesewas or to add up.

### C5. One method does six different jobs (Single Responsibility)

`calculateNetPay` does all of these:

1. finds the employee,
2. works out gross pay,
3. adds overtime,
4. picks a tax band,
5. calculates tax and net pay,
6. prints the result and decides what happens on failure.

Each of these changes for a different reason. HR data, labour rules, tax law and
presentation all live in one method, so you can't test or change one without
touching the rest.

### C6. Tax bands are hard-coded in an `if / else` chain (Open/Closed)

```java
if (gross < 1000) {
    taxRate = 0.05;      // LOW
} else if (gross < 5000) {
    taxRate = 0.15;      // MEDIUM
} else {
    taxRate = 0.25;      // HIGH
}
```

Rosewood is about to add a band "with its own rules". Here, that means editing the
middle of a method that also handles lookup, overtime and printing, and hoping
nothing else breaks. There's a second issue: the chain assumes every band is just
a flat rate on the whole gross. A band with a different rule has nowhere to go.
The band names only exist as comments.

### C7. The repository is hard-wired (Dependency Inversion)

```java
// The calculator makes its own repository, so callers and tests cannot give it another.
private final EmployeeRepository repository = new InMemoryEmployeeRepository();
```

There is an `EmployeeRepository` interface, but the calculator creates the
in-memory version itself, so the interface buys nothing. A test can't hand it a
missing employee or a failing database, and production can't plug in a real one.
This is the main reason the code is hard to test.

### C8. The calculation prints to the console

The `System.out.println` is a side effect sitting in the middle of the maths. The
method only returns a bare `double`, so the printout is the only place the
employee's name shows up. As C2 showed, it's also accidentally acting as the
missing-employee check.

### C9. Magic numbers

`40` is the overtime threshold, `0.5` the overtime premium, `1000` and `5000` the
band limits, and `0.05`, `0.15` and `0.25` the rates. You only know that by reading
around them. Comments like `// LOW` do the job that names should be doing.

### C10. Test data lives in a production class

Every `InMemoryEmployeeRepository` comes pre-loaded with Efua Sarpong (`e1`) and
Kwame Owusu (`e2`), because they're added in the constructor.

### C11. Arguments that are easy to mix up

`calculateNetPay(String employeeId, double hoursWorked, double hourlyRate)`. Hours
and rate are both plain `double`s, so `calculateNetPay("e1", 20, 40)`, with the two
swapped, compiles fine and pays the wrong amount.

### C12. The employee record isn't used in the calculation

The employee is looked up, but only to print their name. The hourly rate comes
from whoever calls the method, so any employee can be paid any rate.

### What I checked and didn't find

I looked for Liskov Substitution and Interface Segregation problems too and
didn't find any. There's no inheritance in the code, and `EmployeeRepository` has
a single method. I'd rather say that than stretch the list to cover all five
SOLID principles.

---

## Part 2 - Tests that pass without testing anything

Most misleading first.

### T1. `calculatesOvertimePay` mocks the class it's supposed to test

```java
PayrollCalculator calculator = mock(PayrollCalculator.class);
when(calculator.calculateNetPay("e2", 50, 30)).thenReturn(1402.5);
assertEquals(1402.5, calculator.calculateNetPay("e2", 50, 30));
```

None of our code runs here. The test tells Mockito to return 1402.5 and then
checks that Mockito returned 1402.5. You could delete the overtime rule and it
would still pass. It's the worst of the three because it looks right: the name
says overtime, and 1402.5 really is the correct answer. Anyone reading it would
assume overtime is covered.

### T2. `returnsPayForAnEmployee` can't fail

```java
double net = calculator.calculateNetPay("e1", 35, 25);
assertNotNull(net);
```

`net` is a primitive `double`. When it's passed to `assertNotNull` it gets boxed
into a `Double`, and a boxed primitive is never `null`. So this passes whatever the
code returns, including the `0` it returns when something has gone wrong.

### T3. `calculatesNetPay` only checks the result is positive

```java
assertTrue(net > 0);
```

This one at least runs real code, but any positive number passes: the wrong tax
rate, the wrong band, broken overtime. The only thing it catches is a result of
zero or less.

### T4. What's never tested at all

- The 15% and 25% bands. Both real test inputs (gross 800 and 875) fall in the 5% band.
- Overtime, in real code.
- The edges of the bands: 999.99, 1000, 4999.99, 5000.
- Anything going wrong: an unknown employee, a `null` id, bad hours, a bad rate.

### T5. The coverage number gives false comfort

10 of 16 lines covered sounds reasonable. But coverage only tells you a line ran,
not that anything checked what it produced. PIT is the better measure here, and
it says the tests miss 13 of 15 planted bugs.
