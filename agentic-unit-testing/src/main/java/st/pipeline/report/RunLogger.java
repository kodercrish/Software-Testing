package st.pipeline.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import st.pipeline.agents.TestExecutorAgent.Verdict;
import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.llm.LlmClient.Call;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves what the report needs in runs/&lt;timestamp&gt;/: every LLM call (prompts, settings, response),
 * the final code, tests and verdict per task, and summary.md over all tasks.
 */
public class RunLogger {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Gson compact = new GsonBuilder().disableHtmlEscaping().create();
    private final Path runDir;
    private final List<Verdict> verdicts = new ArrayList<>();

    public RunLogger(Path runsDir) throws IOException {
        runDir = runsDir.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(runDir);
    }

    public Path runDir() { return runDir; }

    public Path taskDir(Task task) throws IOException {
        return Files.createDirectories(runDir.resolve(task.task_id().replace('/', '_')));
    }

    /** Appends one LLM call as one JSON line to llm-calls.jsonl. */
    public void logCall(Path taskDir, Call call) throws IOException {
        Files.writeString(taskDir.resolve("llm-calls.jsonl"), compact.toJson(call) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void saveFinal(Path taskDir, String code, String tests, Verdict v) throws IOException {
        Files.writeString(taskDir.resolve("Solution.java"), code);
        Files.writeString(taskDir.resolve("SolutionTest.java"), tests);
        Files.writeString(taskDir.resolve("verdict.json"), gson.toJson(v));
        verdicts.add(v);
    }

    /** Writes the per-task results table to summary.md and returns it. */
    public String writeSummary() throws IOException {
        StringBuilder md = new StringBuilder("| Task | Verdict | Tests passed | Statement % | Branch % | Iterations |\n"
                + "|---|---|---|---|---|---|\n");
        for (Verdict v : verdicts)
            md.append(String.format("| %s | %s | %d/%d | %.1f | %.1f | %d |%n", v.taskId(), v.status(),
                    v.testsPassed(), v.testsTotal(), v.statementPct(), v.branchPct(), v.iterations()));
        long pass = verdicts.stream().filter(v -> v.status().equals("PASS")).count();
        md.append(String.format("%nCoverage goal reached with all tests passing: %d/%d tasks%n", pass, verdicts.size()));
        Files.writeString(runDir.resolve("summary.md"), md);
        return md.toString();
    }
}
