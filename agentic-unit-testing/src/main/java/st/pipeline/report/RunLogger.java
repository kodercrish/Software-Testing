package st.pipeline.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import st.pipeline.agents.TestExecutorAgent.Verdict;
import st.pipeline.llm.LlmClient.Call;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Writes everything the report needs into runs/&lt;timestamp&gt;/:
 * settings.json, per-task LLM calls (prompts, parameters, raw responses), final code/tests, verdicts,
 * and summary.csv / summary.md over all tasks.
 */
public class RunLogger {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Gson line = new GsonBuilder().disableHtmlEscaping().create();
    private final Path runDir;
    private final List<Verdict> verdicts = new ArrayList<>();

    public RunLogger(Path runsDir, Properties settings) throws IOException {
        runDir = runsDir.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(runDir);
        Files.writeString(runDir.resolve("settings.json"), gson.toJson(new TreeMap<>(settings)));
    }

    public Path runDir() { return runDir; }

    public Path taskDir(String safeId) throws IOException {
        return Files.createDirectories(runDir.resolve(safeId));
    }

    public void logCall(Path taskDir, Call call) throws IOException {
        Files.writeString(taskDir.resolve("llm-calls.jsonl"), line.toJson(call) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void saveFinal(Path taskDir, String code, String tests, Verdict v) throws IOException {
        if (code != null) Files.writeString(taskDir.resolve("Solution.java"), code);
        if (tests != null) Files.writeString(taskDir.resolve("SolutionTest.java"), tests);
        Files.writeString(taskDir.resolve("verdict.json"), gson.toJson(v));
        verdicts.add(v);
    }

    /** Writes the per-task table and aggregate metrics; returns the markdown summary. */
    public String writeSummary() throws IOException {
        StringBuilder csv = new StringBuilder("task,status,code_correct,tests_passed,tests_total,passed_on_reference,"
                + "line_pct,branch_pct,goal_met,iterations\n");
        StringBuilder md = new StringBuilder("| Task | Status | Code correct | Tests passed | Passed on reference | "
                + "Statement % | Branch % | Goal met | Iterations |\n|---|---|---|---|---|---|---|---|---|\n");
        for (Verdict v : verdicts) {
            csv.append(String.join(",", v.taskId(), v.status(), String.valueOf(v.codeCorrect()),
                    String.valueOf(v.testsPassed()), String.valueOf(v.testsTotal()), String.valueOf(v.testsPassedOnReference()),
                    String.valueOf(v.linePct()), String.valueOf(v.branchPct()), String.valueOf(v.coverageGoalMet()),
                    String.valueOf(v.testIterations()))).append('\n');
            md.append(String.format("| %s | %s | %s | %d/%d | %d/%d | %.1f | %.1f | %s | %d |%n", v.taskId(), v.status(),
                    v.codeCorrect() ? "yes" : "no", v.testsPassed(), v.testsTotal(), v.testsPassedOnReference(), v.testsTotal(),
                    v.linePct(), v.branchPct(), v.coverageGoalMet() ? "yes" : "no", v.testIterations()));
        }
        int n = Math.max(verdicts.size(), 1);
        long goal = verdicts.stream().filter(Verdict::coverageGoalMet).count();
        long correct = verdicts.stream().filter(Verdict::codeCorrect).count();
        long pass = verdicts.stream().filter(v -> v.status().equals("PASS")).count();
        long buggy = verdicts.stream().filter(v -> v.codeCompiles() && !v.codeCorrect()).count();
        long detected = verdicts.stream().filter(v -> v.status().equals("TESTS_FAILED_BUG_DETECTED")).count();
        int totalTests = verdicts.stream().mapToInt(Verdict::testsTotal).sum();
        int validTests = verdicts.stream().mapToInt(Verdict::testsPassedOnReference).sum();
        md.append(String.format("""

                **Aggregate (%d tasks)**
                - Coverage goal met: %d/%d (%.1f%%)
                - Mean statement coverage: %.1f%% | mean branch coverage: %.1f%%
                - Verdict PASS (tests compile, all pass, goal met): %d/%d
                - Generated code correct (official HumanEval-X tests): %d/%d (pass@1 = %.2f)
                - Buggy generated solutions caught by generated tests: %d/%d
                - Test oracle validity (generated tests passing on canonical solution): %d/%d (%.1f%%)
                - Mean test-generation iterations: %.2f
                """, verdicts.size(), goal, verdicts.size(), 100.0 * goal / n,
                verdicts.stream().mapToDouble(Verdict::linePct).average().orElse(0),
                verdicts.stream().mapToDouble(Verdict::branchPct).average().orElse(0),
                pass, verdicts.size(), correct, verdicts.size(), (double) correct / n, detected, buggy,
                validTests, totalTests, totalTests == 0 ? 0 : 100.0 * validTests / totalTests,
                verdicts.stream().mapToInt(Verdict::testIterations).average().orElse(0)));
        Files.writeString(runDir.resolve("summary.csv"), csv);
        Files.writeString(runDir.resolve("summary.md"), md);
        return md.toString();
    }
}
