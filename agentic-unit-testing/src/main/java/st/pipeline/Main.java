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

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Entry point: runs the pipeline on the first 12 HumanEval-X Java problems. Usage: java -jar target/pipeline.jar */
public class Main {
    private static final int NUM_TASKS = 12;

    public static void main(String[] args) throws Exception {
        Properties cfg = new Properties();
        try (Reader r = Files.newBufferedReader(Path.of("config.properties"))) {
            cfg.load(r);
        }
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Set OPENROUTER_API_KEY");
        Criterion criterion = Criterion.valueOf(cfg.getProperty("criterion"));
        double target = Double.parseDouble(cfg.getProperty("target"));

        LlmClient llm = new LlmClient(cfg.getProperty("api.baseUrl"), apiKey, cfg.getProperty("model"),
                Long.parseLong(cfg.getProperty("seed")));
        var codeGen = new CodeGeneratorAgent(llm, params(cfg, "codegen"));
        var testGen = new TestGeneratorAgent(llm, params(cfg, "testgen"), criterion, target);
        var executor = new TestExecutorAgent(Path.of("tools"), criterion, target);
        var logger = new RunLogger(Path.of("runs"));
        var pipeline = new Pipeline(codeGen, testGen, executor, logger,
                Integer.parseInt(cfg.getProperty("maxTestIterations")));

        List<Task> tasks = HumanEvalX.load(Path.of("data/humaneval_java.jsonl")).subList(0, NUM_TASKS);
        System.out.printf("Model %s | goal: %s coverage >= %.0f%% | output: %s%n",
                cfg.getProperty("model"), criterion, target, logger.runDir());
        for (Task task : tasks) {
            System.out.println("== " + task.task_id());
            try {
                Verdict v = pipeline.run(task);
                System.out.println("  VERDICT: " + v.status());
            } catch (Exception e) {
                // e.g. the free model stayed rate-limited: skip this task, continue with the next
                System.out.println("  ERROR: " + e.getMessage());
            }
        }
        System.out.println(logger.writeSummary());
    }

    private static Params params(Properties cfg, String agent) {
        return new Params(Double.parseDouble(cfg.getProperty(agent + ".temperature")),
                Double.parseDouble(cfg.getProperty(agent + ".topP")),
                Integer.parseInt(cfg.getProperty(agent + ".maxTokens")));
    }
}
