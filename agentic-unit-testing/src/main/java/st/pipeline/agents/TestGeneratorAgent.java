package st.pipeline.agents;

import st.pipeline.Criterion;
import st.pipeline.agents.TestExecutorAgent.TestRun;
import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.exec.Reports.Coverage;
import st.pipeline.exec.Reports.Failure;
import st.pipeline.exec.Reports.Line;
import st.pipeline.llm.LlmClient;
import st.pipeline.llm.LlmClient.Call;
import st.pipeline.llm.LlmClient.Params;

import java.util.Map;

/** Agent 2: generates JUnit 5 tests that target a coverage criterion, and refines them using executor feedback. */
public class TestGeneratorAgent {
    private final LlmClient llm;
    private final Params params;
    private final String systemPrompt;

    public TestGeneratorAgent(LlmClient llm, Params params, Criterion criterion, double target) {
        this.llm = llm;
        this.params = params;
        this.systemPrompt = Prompts.render("testgen_system",
                Map.of("criterion", criterion.description, "target", String.valueOf((int) target)));
    }

    public record Output(String tests, Call call) {}

    public Output generate(Task task, String code) throws Exception {
        return ask(initialPrompt(task, code));
    }

    /** Feedback round: sends uncovered lines/branches, failures and compile errors back to the model. */
    public Output refine(Task task, String code, String tests, TestRun run) throws Exception {
        String user = initialPrompt(task, code) + "\n\n"
                + Prompts.render("testgen_feedback", Map.of("feedback", feedback(code, run), "tests", tests));
        return ask(user);
    }

    private String initialPrompt(Task task, String code) {
        return Prompts.render("testgen_user", Map.of("prompt", task.prompt(), "numberedCode", Prompts.withLineNumbers(code)));
    }

    private Output ask(String user) throws Exception {
        Call call = llm.chat("test-generator", systemPrompt, user, params);
        return new Output(Prompts.extractCode(call.response(), "SolutionTest"), call);
    }

    static String feedback(String code, TestRun run) {
        StringBuilder sb = new StringBuilder();
        if (!run.compiled()) {
            String err = run.compileErrors().strip();
            return "SolutionTest.java does not compile:\n```\n" + (err.length() > 3000 ? err.substring(0, 3000) : err) + "\n```";
        }
        String[] lines = code.split("\n", -1);
        Coverage c = run.coverage();
        if (c != null) {
            sb.append(String.format("Coverage: statements %d/%d (%.1f%%), branches %d/%d (%.1f%%)%n",
                    c.linesCovered(), c.linesTotal(), c.linePct(), c.branchesCovered(), c.branchesTotal(), c.branchPct()));
            if (!c.missedLines().isEmpty()) {
                sb.append("Lines never executed:\n");
                for (int nr : c.missedLines()) sb.append(String.format("  line %d: %s%n", nr, src(lines, nr)));
            }
            if (!c.partialBranchLines().isEmpty()) {
                sb.append("Decisions with branch outcomes never taken:\n");
                for (Line l : c.partialBranchLines())
                    sb.append(String.format("  line %d: %s  (%d of %d branch outcomes covered)%n",
                            l.nr(), src(lines, l.nr()), l.cb(), l.cb() + l.mb()));
            }
        }
        var r = run.results();
        sb.append(String.format("Tests: %d run, %d passed, %d failed%n", r.total(), r.passed(), r.failures().size()));
        for (Failure f : r.failures()) sb.append(String.format("  FAILED %s: %s%n", f.test(), f.message()));
        return sb.toString().strip();
    }

    private static String src(String[] lines, int nr) {
        return nr >= 1 && nr <= lines.length ? lines[nr - 1].strip() : "";
    }
}
