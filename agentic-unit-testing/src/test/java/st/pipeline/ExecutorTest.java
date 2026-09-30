package st.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import st.pipeline.agents.Prompts;
import st.pipeline.agents.TestExecutorAgent;
import st.pipeline.agents.TestExecutorAgent.TestRun;
import st.pipeline.dataset.HumanEvalX;
import st.pipeline.dataset.HumanEvalX.Task;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the Test Executor end to end (no LLM needed) using the canonical solution of Java/0. */
class ExecutorTest {
    static final String TESTS = """
            import org.junit.jupiter.api.Test;
            import java.util.*;
            import static org.junit.jupiter.api.Assertions.*;

            class SolutionTest {
                @Test void closePairExists() { assertTrue(new Solution().hasCloseElements(Arrays.asList(1.0, 2.8, 3.0), 0.3)); }
                @Test void noClosePair() { assertFalse(new Solution().hasCloseElements(Arrays.asList(1.0, 2.0, 3.0), 0.5)); }
            }
            """;

    @TempDir Path tmp;

    private final Task task = loadTask();
    private final TestExecutorAgent executor = new TestExecutorAgent(Path.of("tools"), 60, Criterion.BRANCH, 100);

    private static Task loadTask() {
        try { return HumanEvalX.load(Path.of("data/humaneval_java.jsonl")).get(0); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void canonicalSolutionPassesOfficialTests() throws Exception {
        assertTrue(executor.runOfficialTests(task, task.canonicalCode(), tmp));
    }

    @Test
    void measuresFullBranchCoverage() throws Exception {
        TestRun run = executor.runTests(task.canonicalCode(), TESTS, tmp, true);
        assertTrue(run.compiled());
        assertEquals(2, run.results().passed());
        assertEquals(100.0, run.coveragePct());
        assertTrue(executor.goalMet(run));
    }

    @Test
    void worksWithRelativeWorkDir() throws Exception {
        Path rel = Path.of("target/executor-test-" + System.nanoTime());
        assertTrue(executor.compile(task.canonicalCode(), rel).ok());
        assertTrue(executor.runTests(task.canonicalCode(), TESTS, rel.resolve("run"), true).compiled());
    }

    @Test
    void detectsUncoveredBranchesAndFailures() throws Exception {
        String oneTest = TESTS.replaceAll("(?m)^.*noClosePair.*$", "")
                .replace("0.3)); }", "0.01)); }"); // now expects true where the answer is false
        TestRun run = executor.runTests(task.canonicalCode(), oneTest, tmp, true);
        assertEquals(1, run.results().failures().size());
        assertTrue(run.coveragePct() < 100.0);
        assertFalse(run.coverage().partialBranchLines().isEmpty());
    }

    @Test
    void extractsCodeBlockForRequestedClass() {
        String response = "Here:\n```java\npackage x;\nclass Helper {}\n```\n```java\nclass SolutionTest { }\n```";
        assertEquals("class SolutionTest { }\n", Prompts.extractCode(response, "SolutionTest"));
    }
}
