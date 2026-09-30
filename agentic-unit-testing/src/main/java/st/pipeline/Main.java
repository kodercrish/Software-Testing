package st.pipeline;

import st.pipeline.agents.CodeGeneratorAgent;
import st.pipeline.agents.TestExecutorAgent;
import st.pipeline.agents.TestExecutorAgent.Verdict;
import st.pipeline.agents.TestGeneratorAgent;
import st.pipeline.dataset.HumanEvalX;
import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.llm.LlmClient;
import st.pipeline.llm.LlmClient.Params;
import st.pipeline.report.RunLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Entry point. Usage: java -jar target/pipeline.jar [key=value ...]
 * Example:        java -jar target/pipeline.jar tasks=0-4 criterion=STATEMENT
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Config cfg = new Config(Path.of("config.properties"), args);
        Criterion criterion = Criterion.valueOf(cfg.get("criterion").toUpperCase());
        double target = cfg.getDouble("target");

        LlmClient llm = new LlmClient(cfg.get("api.baseUrl"), cfg.apiKey(), cfg.get("model"),
                cfg.list("fallbackModels"), cfg.getInt("seed"), cfg.getInt("requestDelayMs"));
        var codeGen = new CodeGeneratorAgent(llm, new Params(cfg.getDouble("codegen.temperature"),
                cfg.getDouble("codegen.topP"), cfg.getInt("codegen.maxTokens")));
        var testGen = new TestGeneratorAgent(llm, new Params(cfg.getDouble("testgen.temperature"),
                cfg.getDouble("testgen.topP"), cfg.getInt("testgen.maxTokens")), criterion, target);
        var executor = new TestExecutorAgent(Path.of("tools"), cfg.getInt("execTimeoutSeconds"), criterion, target);
        var logger = new RunLogger(Path.of(cfg.get("runsDir")), cfg.asProperties());
        var pipeline = new Pipeline(codeGen, testGen, executor, logger,
                cfg.getInt("maxCodeAttempts"), cfg.getInt("maxTestIterations"));

        Map<Integer, Task> tasks = HumanEvalX.load(Path.of(cfg.get("dataset")));
        System.out.printf("Model %s | goal: %s >= %.0f%% | run dir %s%n", cfg.get("model"), criterion, target, logger.runDir());
        for (int id : cfg.taskIds()) {
            Task task = tasks.get(id);
            if (task == null) { System.out.println("No task " + id); continue; }
            System.out.println("== " + task.id());
            try {
                Verdict v = pipeline.run(task);
                System.out.println("  VERDICT: " + v.status());
            } catch (Exception e) {
                // One failing task (e.g. exhausted rate limit) must not abort the whole run
                System.out.println("  ERROR: " + e.getMessage());
                Files.writeString(logger.taskDir(task.safeId()).resolve("error.log"), String.valueOf(e));
            }
        }
        System.out.println();
        System.out.println(logger.writeSummary());
        System.out.println("Results written to " + logger.runDir());
    }
}
