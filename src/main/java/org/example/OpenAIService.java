package org.example;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String openAiApiKey;

    public OpenAIService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generateAnswer(String context, String question) {
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API Key is missing. Check application.yml configuration.");
        }

        // Prepare prompt for GPT using retrieved documents as context
        String prompt = "Use the following information to answer the question:\n\n"
                + "Context:\n" + context + "\n\n"
                + "Question: " + question + "\n"
                + "Answer:";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4",  // Use "gpt-3.5-turbo" if needed
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful AI assistant."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) ((Map<String, Object>) ((List<Map<String, Object>>) response.get("choices")).get(0).get("message")).get("content");
    }
}
