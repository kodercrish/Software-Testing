package st.pipeline.agents;

import st.pipeline.Criterion;
import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.exec.Proc;
import st.pipeline.exec.Reports;
import st.pipeline.exec.Reports.Coverage;
import st.pipeline.exec.Reports.TestResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Agent 3: compiles the code and tests, runs the tests with JUnit in a separate JVM while JaCoCo records
 * coverage, and gives a verdict. The verdict comes from real execution, not from LLM judgement.
 */
public class TestExecutorAgent {
    private static final int TIMEOUT_SECONDS = 30;

    private final Path tools;
    private final Criterion criterion;
    private final double target;

    public TestExecutorAgent(Path toolsDir, Criterion criterion, double target) {
        this.tools = toolsDir.toAbsolutePath();
        this.criterion = criterion;
        this.target = target;
    }

    /** Result of one execution of the generated tests. */
    public record TestRun(boolean codeCompiles, boolean compiled, String compileErrors,
                          TestResults results, Coverage coverage, double coveragePct) {}

    public record Verdict(String taskId, String status, int testsTotal, int testsPassed, String criterion,
                          double target, double statementPct, double branchPct, int iterations) {}

    public TestRun runTests(String code, String tests, Path dir) throws Exception {
        dir = dir.toAbsolutePath(); // commands run inside dir, so relative paths would resolve twice
        Path src = dir.resolve("src"), main = dir.resolve("out/main"), test = dir.resolve("out/test");
        write(src, "Solution.java", code);
        write(src, "SolutionTest.java", tests);
        String junit = tools.resolve("junit-console.jar").toString();

        // 1. Compile the code under test, then the tests
        Proc.Result c1 = Proc.run(List.of("javac", "-g", "-d", main.toString(), src.resolve("Solution.java").toString()),
                dir, TIMEOUT_SECONDS);
        if (!c1.ok()) return failedCompile(dir, false, c1.output());
        Proc.Result c2 = Proc.run(List.of("javac", "-g", "-d", test.toString(), "-cp", Proc.classpath(main, Path.of(junit)),
                src.resolve("SolutionTest.java").toString()), dir, TIMEOUT_SECONDS);
        if (!c2.ok()) return failedCompile(dir, true, c2.output());

        // 2. Run the tests with the JaCoCo agent attached
        Path exec = dir.resolve("jacoco.exec");
        Proc.Result run = Proc.run(List.of("java", "-javaagent:" + tools.resolve("jacocoagent.jar") + "=destfile=" + exec,
                "-jar", junit, "execute", "--disable-banner", "--details=tree",
                "--class-path", Proc.classpath(main, test), "--select-class", "SolutionTest",
                "--reports-dir", dir.resolve("junit").toString()), dir, TIMEOUT_SECONDS);
        Files.writeString(dir.resolve("junit-output.log"), run.output());
        TestResults results = Reports.parseJUnit(dir.resolve("junit"));

        // 3. Turn the coverage data of class Solution into an XML report and read it
        Path xml = dir.resolve("coverage.xml");
        Proc.run(List.of("java", "-jar", tools.resolve("jacococli.jar").toString(), "report", exec.toString(),
                "--classfiles", main.toString(), "--xml", xml.toString()), dir, TIMEOUT_SECONDS);
        if (!Files.exists(xml)) return new TestRun(true, true, "", results, null, 0); // e.g. test run timed out
        Coverage cov = Reports.parseJacoco(xml);
        return new TestRun(true, true, "", results, cov, criterion.measure(cov));
    }

    /** True when all tests pass and the coverage goal is reached. */
    public boolean done(TestRun run) {
        return run.compiled() && run.results().total() > 0 && run.results().failures().isEmpty()
                && run.coveragePct() >= target;
    }

    public Verdict verdict(Task task, TestRun run, int iterations) {
        String status;
        if (!run.compiled()) status = "COMPILE_ERROR";
        else if (run.results().total() == 0 || !run.results().failures().isEmpty()) status = "TESTS_FAILED";
        else if (run.coveragePct() < target) status = "COVERAGE_NOT_MET";
        else status = "PASS";
        Coverage c = run.coverage();
        return new Verdict(task.task_id(), status, run.results().total(), run.results().passed(), criterion.name(),
                target, c == null ? 0 : round(c.linePct()), c == null ? 0 : round(c.branchPct()), iterations);
    }

    private static TestRun failedCompile(Path dir, boolean codeCompiles, String errors) throws IOException {
        Files.writeString(dir.resolve("javac.log"), errors);
        return new TestRun(codeCompiles, false, errors, TestResults.none(), null, 0);
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }

    private static void write(Path dir, String name, String content) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), content);
    }
}
