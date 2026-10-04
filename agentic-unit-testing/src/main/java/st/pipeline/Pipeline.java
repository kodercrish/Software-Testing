package st.pipeline;

import st.pipeline.agents.CodeGeneratorAgent;
import st.pipeline.agents.TestExecutorAgent;
import st.pipeline.agents.TestExecutorAgent.TestRun;
import st.pipeline.agents.TestExecutorAgent.Verdict;
import st.pipeline.agents.TestGeneratorAgent;
import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.report.RunLogger;

import java.nio.file.Path;

/**
 * Connects the three agents for one task (plain Java control flow, no agent framework):
 * <pre>
 *   spec --> [Code Generator] --code--> [Test Generator] --tests--> [Test Executor] --> verdict
 *                                              ^                           |
 *                                              +-- uncovered lines/branches, failures (feedback)
 * </pre>
 */
public class Pipeline {
    private final CodeGeneratorAgent codeGen;
    private final TestGeneratorAgent testGen;
    private final TestExecutorAgent executor;
    private final RunLogger logger;
    private final int maxTestIterations;

    public Pipeline(CodeGeneratorAgent codeGen, TestGeneratorAgent testGen, TestExecutorAgent executor,
                    RunLogger logger, int maxTestIterations) {
        this.codeGen = codeGen;
        this.testGen = testGen;
        this.executor = executor;
        this.logger = logger;
        this.maxTestIterations = maxTestIterations;
    }

    public Verdict run(Task task) throws Exception {
        Path dir = logger.taskDir(task);

        // Agent 1: generate the code under test
        CodeGeneratorAgent.Output code = codeGen.generate(task);
        logger.logCall(dir, code.call());

        // Agent 2: generate tests for the coverage goal; Agent 3: execute them
        TestGeneratorAgent.Output tests = testGen.generate(task, code.code());
        logger.logCall(dir, tests.call());
        TestRun run = executor.runTests(code.code(), tests.tests(), dir.resolve("iter1"));
        print(1, run);

        // Feedback loop: send uncovered lines/branches and failures back until the goal is met
        int iter = 1;
        while (!executor.done(run) && run.codeCompiles() && iter < maxTestIterations) {
            iter++;
            tests = testGen.refine(task, code.code(), tests.tests(), run);
            logger.logCall(dir, tests.call());
            run = executor.runTests(code.code(), tests.tests(), dir.resolve("iter" + iter));
            print(iter, run);
        }

        Verdict v = executor.verdict(task, run, iter);
        logger.saveFinal(dir, code.code(), tests.tests(), v);
        return v;
    }

    private static void print(int iter, TestRun r) {
        if (!r.compiled()) System.out.printf("  iter %d: does not compile%n", iter);
        else System.out.printf("  iter %d: %d/%d tests pass, coverage %.1f%%%n",
                iter, r.results().passed(), r.results().total(), r.coveragePct());
    }
}
