package ai.athena.examiner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

/**
 * Thin client for the OpenAI Chat Completions API (vision-capable models).
 * No SDK — one endpoint, JSON in, JSON out.
 *
 * Spec: primary GPT-5.5, fallback GPT-4o. Model is env-configurable
 * (OPENAI_MODEL), so upgrading is a config change, not a code change.
 */
@Component
public class OpenAiClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient http;
    private final String apiKey;
    private final String model;

    public OpenAiClient(@Value("${athena.openai.base-url}") String baseUrl,
                        @Value("${athena.openai.api-key}") String apiKey,
                        @Value("${athena.openai.model}") String model) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends a prompt plus zero or more PNG pages; instructs the model to reply
     * with pure JSON (json_object mode) and returns the parsed root node.
     */
    public JsonNode generateJson(String prompt, List<byte[]> pngPages) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.1);

        ObjectNode responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_object");
        root.set("response_format", responseFormat);

        ArrayNode content = mapper.createArrayNode();
        content.add(mapper.createObjectNode()
                .put("type", "text")
                .put("text", prompt));
        for (byte[] png : pngPages) {
            ObjectNode imageUrl = mapper.createObjectNode();
            imageUrl.put("url", "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(png));
            imageUrl.put("detail", "high");   // handwriting needs full resolution
            ObjectNode part = mapper.createObjectNode();
            part.put("type", "image_url");
            part.set("image_url", imageUrl);
            content.add(part);
        }

        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.set("content", content);
        root.set("messages", mapper.createArrayNode().add(message));

        String body = http.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(root.toString())
                .retrieve()
                .body(String.class);

        try {
            JsonNode resp = mapper.readTree(body);
            String text = resp.path("choices").path(0)
                              .path("message").path("content").asText("");
            if (text.isBlank()) {
                throw new IllegalStateException("OpenAI returned no content: " + abbreviate(body));
            }
            return mapper.readTree(text);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAI response: " + abbreviate(body), e);
        }
    }

    private static String abbreviate(String s) {
        if (s == null) return "null";
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
