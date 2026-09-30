package st.pipeline.llm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal client for an OpenAI-compatible /chat/completions endpoint (OpenRouter by default).
 * Plain HTTP + JSON: no agent framework, no retrieval, no chain-of-thought prompting.
 */
public class LlmClient {
    private static final int MAX_RETRIES = 8;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private final Gson gson = new Gson();
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final List<String> fallbackModels;
    private final long seed;
    private final long delayMs;

    public LlmClient(String baseUrl, String apiKey, String model, List<String> fallbackModels, long seed, long delayMs) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.model = model;
        this.fallbackModels = fallbackModels;
        this.seed = seed;
        this.delayMs = delayMs;
    }

    /** Settings sent with a request; logged so they can be reported. */
    public record Params(double temperature, double topP, int maxTokens) {}

    /** One complete LLM interaction, as recorded in the run log. */
    public record Call(String agent, String model, Params params, long seed,
                       String systemPrompt, String userPrompt, String response, long latencyMs) {}

    public Call chat(String agent, String systemPrompt, String userPrompt, Params p) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "model", model,
                "temperature", p.temperature(),
                "top_p", p.topP(),
                "max_tokens", p.maxTokens(),
                "seed", seed,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt))));
        // OpenRouter model routing: if the primary model is rate-limited, the next one in the list is used
        if (!fallbackModels.isEmpty()) {
            List<String> models = new ArrayList<>(List.of(model));
            models.addAll(fallbackModels);
            body.put("models", models);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        for (int attempt = 1; ; attempt++) {
            Thread.sleep(delayMs);
            long start = System.currentTimeMillis();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            String content = resp.statusCode() == 200 ? extract(resp.body(), "content") : null;
            if (content != null && !content.isBlank()) {
                String used = extract(resp.body(), "model"); // the model that actually answered
                return new Call(agent, used != null ? used : model, p, seed, systemPrompt, userPrompt, content, latency);
            }

            // Rate limits (429), provider errors (5xx) and empty completions are retried with backoff
            boolean retryable = resp.statusCode() == 429 || resp.statusCode() >= 500 || resp.statusCode() == 200;
            if (!retryable || attempt == MAX_RETRIES)
                throw new IOException("LLM request failed (HTTP " + resp.statusCode() + "): " + resp.body());
            long backoff = Math.min(10_000L * attempt, 60_000L);
            System.out.printf("    [%s] HTTP %d, retrying in %ds...%n", agent, resp.statusCode(), backoff / 1000);
            Thread.sleep(backoff);
        }
    }

    /** Reads choices[0].message.content (field "content") or the top-level "model" field of a response. */
    private static String extract(String json, String field) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (field.equals("model")) return root.has("model") ? root.get("model").getAsString() : null;
            if (!root.has("choices") || root.getAsJsonArray("choices").isEmpty()) return null;
            var message = root.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");
            return message.has("content") && !message.get("content").isJsonNull() ? message.get("content").getAsString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
