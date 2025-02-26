
package org.example;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class EmbeddingUtil {

        private  WebClient webClient;

        @Value("${openai.api-key}")
        private String openAiApiKey;
        @Value("${openai.api-url}")
        private String openAiApiUrl;

    @PostConstruct
    private void init() {
        this.webClient = WebClient.builder()
                .baseUrl(openAiApiUrl)  // Ensure this is "https://api.openai.com/v1"
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

        // Convert method to return Mono instead of blocking
        public Mono<float[]> computeEmbedding(String text) {
            if (openAiApiKey == null || openAiApiKey.isEmpty()) {
                return Mono.error(new RuntimeException("OpenAI API Key is missing. Check application.yml configuration."));
            }

            Map<String, Object> requestBody = Map.of(
                    "input", text,
                    "model", "text-embedding-ada-002"
            );

            return webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> {
                        List<Double> embeddingList = (List<Double>) ((List<Map<String, Object>>) response.get("data")).get(0).get("embedding");
                        float[] embedding = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            embedding[i] = embeddingList.get(i).floatValue();
                        }
                        return normalize(embedding);
                    });
        }

        private float[] normalize(float[] vector) {
            double sum = 0.0;
            for (float val : vector) {
                sum += val * val;
            }
            double magnitude = Math.sqrt(sum);
            if (magnitude == 0) return vector;
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
            return vector;
        }
}
