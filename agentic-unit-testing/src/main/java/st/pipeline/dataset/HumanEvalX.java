package st.pipeline.dataset;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loader for the Java split of HumanEval-X (one JSON object per line). */
public final class HumanEvalX {

    /**
     * One HumanEval-X problem.
     * prompt              = imports + class Solution + Javadoc spec + method signature (input to the code generator)
     * canonicalSolution   = reference method body (completes prompt)
     * test                = official hidden tests as a Main class (used only as ground truth, never shown to the LLM)
     */
    public record Task(String task_id, String prompt, String declaration, String canonical_solution,
                       String test, String text) {
        public String id() { return task_id; }
        public String safeId() { return task_id.replace('/', '_'); }
        public String canonicalCode() { return prompt + canonical_solution; }
    }

    private HumanEvalX() {}

    public static Map<Integer, Task> load(Path file) throws IOException {
        Gson gson = new Gson();
        List<Task> tasks = Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank())
                .map(l -> gson.fromJson(l, Task.class))
                .toList();
        return tasks.stream().collect(Collectors.toMap(
                t -> Integer.parseInt(t.task_id().substring(t.task_id().indexOf('/') + 1)),
                Function.identity()));
    }
}
