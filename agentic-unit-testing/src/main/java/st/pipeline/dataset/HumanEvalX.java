package st.pipeline.dataset;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Loader for the Java split of HumanEval-X (one JSON object per line). */
public final class HumanEvalX {

    /** One problem. prompt = imports + class Solution + Javadoc specification + method signature. */
    public record Task(String task_id, String prompt) {}

    private HumanEvalX() {}

    public static List<Task> load(Path file) throws IOException {
        Gson gson = new Gson();
        return Files.readAllLines(file).stream()
                .filter(l -> !l.isBlank())
                .map(l -> gson.fromJson(l, Task.class))
                .toList();
    }
}
