package st.pipeline.agents;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Prompt templates (src/main/resources/prompts) and extraction of code from LLM responses. */
public final class Prompts {
    private static final Pattern CODE_BLOCK = Pattern.compile("```(?:java)?\\s*\\n(.*?)```", Pattern.DOTALL);

    private Prompts() {}

    /** Loads prompts/{name}.txt and replaces {{key}} placeholders. */
    public static String render(String name, Map<String, String> vars) {
        String text;
        try (InputStream in = Prompts.class.getResourceAsStream("/prompts/" + name + ".txt")) {
            if (in == null) throw new IllegalArgumentException("No prompt template: " + name);
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        for (var e : vars.entrySet()) text = text.replace("{{" + e.getKey() + "}}", e.getValue());
        return text.strip();
    }

    /** Returns the fenced code block that declares {@code className}, or the whole response if there is none. */
    public static String extractCode(String response, String className) {
        Matcher m = CODE_BLOCK.matcher(response);
        String first = null;
        while (m.find()) {
            String block = m.group(1);
            if (first == null) first = block;
            if (block.matches("(?s).*\\bclass\\s+" + className + "\\b.*")) return clean(block);
        }
        return clean(first != null ? first : response);
    }

    private static String clean(String code) {
        return code.replaceAll("(?m)^\\s*package\\s+[\\w.]+\\s*;\\s*$", "").strip() + "\n";
    }

    public static String withLineNumbers(String code) {
        String[] lines = code.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) sb.append(String.format("%3d| %s%n", i + 1, lines[i]));
        return sb.toString();
    }
}
