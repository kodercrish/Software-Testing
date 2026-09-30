package st.pipeline;

import st.pipeline.exec.Reports.Coverage;

/** Coverage criterion the test generator must satisfy, measured with JaCoCo. */
public enum Criterion {
    /** Every executable statement (JaCoCo LINE counter) is executed by at least one test. */
    STATEMENT("statement coverage: every executable statement/line of the method is executed by at least one test"),
    /** Every decision outcome (true and false of each if/loop/ternary/&&/||) is taken (JaCoCo BRANCH counter). */
    BRANCH("branch (decision) coverage: every decision (if, loop condition, ternary, &&, ||, switch case) evaluates "
            + "both to true and to false at least once, so every branch outcome is exercised");

    public final String description;

    Criterion(String description) { this.description = description; }

    public double measure(Coverage c) {
        return this == STATEMENT ? c.linePct() : c.branchPct();
    }
}
