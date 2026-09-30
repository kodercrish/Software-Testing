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
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 3: executes code and tests in a separate JVM and produces a verdict.
 * The verdict is computed from real execution (compiler, JUnit, JaCoCo), not from LLM judgement,
 * so it is objective and reproducible.
 */
public class TestExecutorAgent {
    private final Path tools;
    private final int timeout;
    private final Criterion criterion;
    private final double target;

    public TestExecutorAgent(Path toolsDir, int timeoutSeconds, Criterion criterion, double target) {
        this.tools = toolsDir.toAbsolutePath();
        this.timeout = timeoutSeconds;
        this.criterion = criterion;
        this.target = target;
        for (String jar : List.of("junit-console.jar", "jacocoagent.jar", "jacococli.jar"))
            if (!Files.exists(tools.resolve(jar)))
                throw new IllegalStateException("Missing " + tools.resolve(jar) + " - run 'mvn package' first");
    }

    public record CompileResult(boolean ok, String errors) {}

    /** Result of running the generated test class. */
    public record TestRun(boolean compiled, String compileErrors, TestResults results,
                          Coverage coverage, double coveragePct) {}

    public record Verdict(
            String taskId,
            boolean codeCompiles,
            boolean codeCorrect,             // official HumanEval-X tests pass on generated code (ground truth)
            boolean testsCompile,
            int testsTotal,
            int testsPassed,                 // generated tests passing on the generated code
            int testsPassedOnReference,      // generated tests passing on the canonical solution (oracle validity)
            String criterion,
            double target,
            double coveragePct,
            double linePct,
            double branchPct,
            boolean coverageGoalMet,
            int testIterations,
            String status) {}

    /** Compiles the generated Solution on its own (used to give compile errors back to the code generator). */
    public CompileResult compile(String code, Path dir) throws IOException, InterruptedException {
        dir = dir.toAbsolutePath();
        Path src = write(dir.resolve("src"), "Solution.java", code);
        Proc.Result r = Proc.run(List.of("javac", "-g", "-d", dir.resolve("out").toString(), src.toString()), dir, timeout);
        Files.writeString(dir.resolve("javac.log"), r.output());
        return new CompileResult(r.ok(), r.output());
    }

    /** Runs the official HumanEval-X tests (class Main) against the code. Exit code 0 means correct. */
    public boolean runOfficialTests(Task task, String code, Path dir) throws IOException, InterruptedException {
        dir = dir.toAbsolutePath();
        // The dataset test relies on the imports of the solution file, so both go in one compilation unit
        String unit = code.replaceFirst("public\\s+class\\s+Solution", "class Solution") + "\n\n" + task.test();
        Path src = write(dir, "Main.java", unit);
        Path out = dir.resolve("out");
        Proc.Result c = Proc.run(List.of("javac", "-d", out.toString(), src.toString()), dir, timeout);
        if (!c.ok()) {
            Files.writeString(dir.resolve("official-tests.log"), c.output());
            return false;
        }
        Proc.Result r = Proc.run(List.of("java", "-cp", out.toString(), "Main"), dir, timeout);
        Files.writeString(dir.resolve("official-tests.log"), r.output());
        return r.ok();
    }

    /** Compiles code + JUnit tests, runs them with the JaCoCo agent and parses test and coverage reports. */
    public TestRun runTests(String code, String tests, Path dir, boolean measureCoverage) throws Exception {
        dir = dir.toAbsolutePath(); // commands run inside dir, so relative paths would resolve twice
        Path src = dir.resolve("src");
        Path main = dir.resolve("out/main"), test = dir.resolve("out/test");
        write(src, "Solution.java", code);
        write(src, "SolutionTest.java", tests);
        String junit = tools.resolve("junit-console.jar").toString();

        Proc.Result c1 = Proc.run(List.of("javac", "-g", "-d", main.toString(), src.resolve("Solution.java").toString()), dir, timeout);
        if (!c1.ok()) {
            Files.writeString(dir.resolve("javac.log"), c1.output());
            return new TestRun(false, c1.output(), TestResults.none(), null, 0);
        }
        Proc.Result c2 = Proc.run(List.of("javac", "-g", "-d", test.toString(), "-cp", Proc.classpath(main, Path.of(junit)),
                src.resolve("SolutionTest.java").toString()), dir, timeout);
        if (!c2.ok()) {
            Files.writeString(dir.resolve("javac.log"), c2.output());
            return new TestRun(false, c2.output(), TestResults.none(), null, 0);
        }

        Path exec = dir.resolve("jacoco.exec");
        List<String> cmd = new ArrayList<>(List.of("java"));
        if (measureCoverage) cmd.add("-javaagent:" + tools.resolve("jacocoagent.jar") + "=destfile=" + exec);
        cmd.addAll(List.of("-jar", junit, "execute", "--disable-banner", "--details=tree",
                "--class-path", Proc.classpath(main, test), "--select-class", "SolutionTest",
                "--reports-dir", dir.resolve("junit").toString()));
        Proc.Result run = Proc.run(cmd, dir, timeout);
        Files.writeString(dir.resolve("junit-output.log"), run.output());
        TestResults results = Reports.parseJUnit(dir.resolve("junit"));
        if (run.timedOut())
            results = new TestResults(results.total(), 0, List.of(new Reports.Failure("*", "Test run timed out after " + timeout + "s")));
        if (!measureCoverage || !Files.exists(exec)) return new TestRun(true, "", results, null, 0);

        Path xml = dir.resolve("coverage.xml");
        Proc.run(List.of("java", "-jar", tools.resolve("jacococli.jar").toString(), "report", exec.toString(),
                "--classfiles", main.toString(), "--sourcefiles", src.toString(),
                "--xml", xml.toString(), "--html", dir.resolve("coverage-html").toString()), dir, timeout);
        Coverage cov = Reports.parseJacoco(xml);
        return new TestRun(true, "", results, cov, criterion.measure(cov));
    }

    public boolean goalMet(TestRun run) {
        return run.compiled() && run.coverage() != null && run.coveragePct() >= target;
    }

    public Verdict verdict(Task task, boolean codeCompiles, boolean codeCorrect, TestRun run,
                           TestRun onReference, int iterations) {
        boolean goal = goalMet(run);
        int total = run.results().total(), passed = run.results().passed();
        String status;
        if (!codeCompiles) status = "CODE_DOES_NOT_COMPILE";
        else if (!run.compiled()) status = "TESTS_DO_NOT_COMPILE";
        else if (total == 0) status = "NO_TESTS_RUN";
        else if (passed < total) status = codeCorrect ? "TESTS_FAILED" : "TESTS_FAILED_BUG_DETECTED";
        else if (!goal) status = "COVERAGE_GOAL_NOT_MET";
        else status = "PASS";
        Coverage cov = run.coverage();
        return new Verdict(task.id(), codeCompiles, codeCorrect, run.compiled(), total, passed,
                onReference == null ? 0 : onReference.results().passed(),
                criterion.name(), target, round(run.coveragePct()),
                cov == null ? 0 : round(cov.linePct()), cov == null ? 0 : round(cov.branchPct()),
                goal, iterations, status);
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }

    private static Path write(Path dir, String name, String content) throws IOException {
        Files.createDirectories(dir);
        return Files.writeString(dir.resolve(name), content);
    }
}
