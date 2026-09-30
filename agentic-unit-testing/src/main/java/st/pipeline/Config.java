package st.pipeline;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Pipeline settings from config.properties; any key can be overridden on the command line as key=value. */
public class Config {
    private final Properties props = new Properties();

    public Config(Path file, String[] overrides) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            props.load(r);
        }
        for (String arg : overrides) {
            int eq = arg.indexOf('=');
            if (eq <= 0) throw new IllegalArgumentException("Expected key=value, got: " + arg);
            props.setProperty(arg.substring(0, eq), arg.substring(eq + 1));
        }
    }

    public String get(String key) {
        String v = props.getProperty(key);
        if (v == null) throw new IllegalArgumentException("Missing config key: " + key);
        return v.trim();
    }

    public int getInt(String key) { return Integer.parseInt(get(key)); }

    public double getDouble(String key) { return Double.parseDouble(get(key)); }

    /** Comma-separated value as a list; empty list for an empty value. */
    public List<String> list(String key) {
        String v = props.getProperty(key, "").trim();
        return v.isEmpty() ? List.of() : java.util.Arrays.stream(v.split(",")).map(String::trim).toList();
    }

    public String apiKey() {
        String key = System.getenv(get("api.keyEnv"));
        if (key == null || key.isBlank())
            throw new IllegalStateException("Set the environment variable " + get("api.keyEnv") + " to your API key");
        return key;
    }

    /** Parses "0-3,7" into [0,1,2,3,7]. */
    public List<Integer> taskIds() {
        List<Integer> ids = new ArrayList<>();
        for (String part : get("tasks").split(",")) {
            String[] range = part.trim().split("-");
            int from = Integer.parseInt(range[0].trim());
            int to = range.length > 1 ? Integer.parseInt(range[1].trim()) : from;
            for (int i = from; i <= to; i++) ids.add(i);
        }
        return ids;
    }

    public Properties asProperties() { return props; }
}
