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
import java.util.List;
import java.util.Map;

/** Minimal client for an OpenAI-compatible /chat/completions endpoint (OpenRouter). Plain HTTP + JSON. */
public class LlmClient {
    private static final int MAX_ATTEMPTS = 6;
    private static final long RETRY_WAIT_MS = 20_000;

    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final long seed;

    public LlmClient(String baseUrl, String apiKey, String model, long seed) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.seed = seed;
    }

    /** Sampling settings of an agent. */
    public record Params(double temperature, double topP, int maxTokens) {}

    /** One LLM interaction, as recorded in the run log. */
    public record Call(String agent, String model, Params params, long seed,
                       String systemPrompt, String userPrompt, String response) {}

    public Call chat(String agent, String systemPrompt, String userPrompt, Params p) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", p.temperature(),
                "top_p", p.topP(),
                "max_tokens", p.maxTokens(),
                "seed", seed,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        // Free models are often rate-limited (HTTP 429) or return an empty reply: wait and try again
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            String content = resp.statusCode() == 200 ? content(resp.body()) : null;
            if (content != null && !content.isBlank())
                return new Call(agent, model, p, seed, systemPrompt, userPrompt, content);
            System.out.printf("    [%s] HTTP %d, retrying in %ds...%n", agent, resp.statusCode(), RETRY_WAIT_MS / 1000);
            Thread.sleep(RETRY_WAIT_MS);
        }
        throw new IOException("LLM request failed after " + MAX_ATTEMPTS + " attempts");
    }

    /** Extracts choices[0].message.content from the JSON response. */
    private static String content(String json) {
        try {
            JsonObject message = JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");
            return message.get("content").isJsonNull() ? null : message.get("content").getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
