package st.pipeline;

import st.pipeline.agents.CodeGeneratorAgent;
import st.pipeline.agents.TestExecutorAgent;
import st.pipeline.agents.TestExecutorAgent.CompileResult;
import st.pipeline.agents.TestExecutorAgent.TestRun;
import st.pipeline.agents.TestExecutorAgent.Verdict;
import st.pipeline.agents.TestGeneratorAgent;
import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.exec.Reports.TestResults;
import st.pipeline.report.RunLogger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Orchestrates the three agents for one task:
 * <pre>
 *   spec --> [Code Generator] --code--> [Test Generator] --tests--> [Test Executor] --verdict
 *                 ^  compile errors  |           ^   uncovered lines/branches, failures   |
 *                 +------------------+           +----------------------------------------+
 * </pre>
 * The control flow is a plain loop; no agent framework is used.
 */
public class Pipeline {
    private final CodeGeneratorAgent codeGen;
    private final TestGeneratorAgent testGen;
    private final TestExecutorAgent executor;
    private final RunLogger logger;
    private final int maxCodeAttempts;
    private final int maxTestIterations;

    public Pipeline(CodeGeneratorAgent codeGen, TestGeneratorAgent testGen, TestExecutorAgent executor,
                    RunLogger logger, int maxCodeAttempts, int maxTestIterations) {
        this.codeGen = codeGen;
        this.testGen = testGen;
        this.executor = executor;
        this.logger = logger;
        this.maxCodeAttempts = maxCodeAttempts;
        this.maxTestIterations = maxTestIterations;
    }

    public Verdict run(Task task) throws Exception {
        Path dir = logger.taskDir(task.safeId());

        // Agent 1: code generation, with compile-error feedback
        CodeGeneratorAgent.Output code = codeGen.generate(task);
        logger.logCall(dir, code.call());
        CompileResult compiled = executor.compile(code.code(), dir.resolve("codegen/attempt1"));
        for (int a = 2; !compiled.ok() && a <= maxCodeAttempts; a++) {
            System.out.println("  code does not compile, asking for a fix (attempt " + a + ")");
            code = codeGen.fix(task, code.code(), compiled.errors());
            logger.logCall(dir, code.call());
            compiled = executor.compile(code.code(), dir.resolve("codegen/attempt" + a));
        }
        if (!compiled.ok()) {
            TestRun none = new TestRun(false, "", TestResults.none(), null, 0);
            Verdict v = executor.verdict(task, false, false, none, null, 0);
            logger.saveFinal(dir, code.code(), null, v);
            return v;
        }
        // Ground truth for the report only; the result is not given to any agent
        boolean correct = executor.runOfficialTests(task, code.code(), dir.resolve("official-tests"));
        System.out.println("  code generated (official tests: " + (correct ? "pass" : "FAIL") + ")");

        // Agents 2 + 3: generate tests, execute, feed coverage gaps back until the goal is met
        TestGeneratorAgent.Output tests = testGen.generate(task, code.code());
        logger.logCall(dir, tests.call());
        TestRun run = executor.runTests(code.code(), tests.tests(), dir.resolve("iter1"), true);
        print(1, run);
        int iter = 1, bestIter = 1;
        String bestTests = tests.tests();
        TestRun best = run;
        while (!done(best) && iter < maxTestIterations) {
            iter++;
            tests = testGen.refine(task, code.code(), bestTests, best);
            logger.logCall(dir, tests.call());
            run = executor.runTests(code.code(), tests.tests(), dir.resolve("iter" + iter), true);
            print(iter, run);
            // A refinement round can make things worse (e.g. tests stop compiling): keep the best suite so far
            if (score(run) >= score(best)) { best = run; bestTests = tests.tests(); bestIter = iter; }
        }
        run = best;

        // Oracle check: do the generated tests accept the canonical (known-correct) solution?
        TestRun onReference = run.compiled()
                ? executor.runTests(task.canonicalCode(), bestTests, dir.resolve("reference"), false) : null;
        Verdict v = executor.verdict(task, true, correct, run, onReference, iter);
        logger.saveFinal(dir, code.code(), bestTests, v);
        Files.writeString(dir.resolve("final-iteration.txt"), "iter" + bestIter + "\n");
        return v;
    }

    private boolean done(TestRun run) {
        return executor.goalMet(run) && run.results().total() > 0 && run.results().failures().isEmpty();
    }

    /** Orders suites: compiling > all tests passing > higher coverage. */
    private static double score(TestRun r) {
        if (!r.compiled()) return -1;
        return (r.results().failures().isEmpty() && r.results().total() > 0 ? 1000 : 0) + r.coveragePct();
    }

    private static void print(int iter, TestRun r) {
        if (!r.compiled()) System.out.printf("  iter %d: tests do not compile%n", iter);
        else System.out.printf("  iter %d: %d/%d tests pass, coverage %.1f%%%n",
                iter, r.results().passed(), r.results().total(), r.coveragePct());
    }
}
