
package org.example;

import jakarta.annotation.PostConstruct;
import org.example.EmbeddingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PineconeService {

    private WebClient webClient;
    @Autowired
    private EmbeddingUtil embeddingUtil;

    @Value("${pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${pinecone.index-url}")
    private String pineconeIndexUrl;
    @PostConstruct
    private void init() {

        this.webClient = WebClient.builder()
                .baseUrl(pineconeIndexUrl)
                .defaultHeader("Api-Key", pineconeApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<Void> upsertVector(String id, String text) {
        return embeddingUtil.computeEmbedding(text)
                .flatMap(vector -> {
                    Map<String, Object> vectorData = Map.of(
                            "id", id,
                            "values", vector,
                            "metadata", Map.of(
                                    "invoice_number", id,
                                    "billed_to", extractBilledTo(text),
                                    "date", extractDate(text),
                                    "total", extractTotalAmount(text),
                                    "text", text
                            )
                    );

                    Map<String, Object> payload = Map.of(
                            "vectors", List.of(vectorData),
                            "namespace", ""
                    );

                    return webClient.post()
                            .uri("/vectors/upsert")
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(Void.class);
                });
    }



    public Mono<List<String>> queryDocuments(String invoiceNumber, int topK) {
        return embeddingUtil.computeEmbedding(invoiceNumber)
                .flatMap(vector -> {
                    // Construct query payload as per Pinecone API
                    Map<String, Object> payload = Map.of(
                            "vector", vector,
                            "topK", topK,
                            "includeMetadata", true
                    );

                    return webClient.post()
                            .uri("/query")
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(Map.class);
                })
                .map(response -> {
                    if (response == null || !response.containsKey("matches")) {
                        return Collections.singletonList("No matches found.");
                    }

                    List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
                    List<String> retrievedTexts = new ArrayList<>();

                    for (Map<String, Object> match : matches) {
                        Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");

                        if (metadata != null && metadata.containsKey("text")) {
                            retrievedTexts.add((String) metadata.get("text"));
                        }
                    }

                    return retrievedTexts;
                });
    }


    private String extractBilledTo(String text) {
        Pattern pattern = Pattern.compile("Billed To:\\s*(.*?)\\n", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Unknown";
    }
    private String extractTotalAmount(String text) {
        Pattern pattern = Pattern.compile("Total:\\s*\\$(\\d+\\.\\d{2})");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "0.00";
    }
    private String extractDate(String text) {
        Pattern pattern = Pattern.compile("Date:\\s*(\\d{4}-\\d{2}-\\d{2})|Date:\\s*(\\d{2}/\\d{2}/\\d{4})|Date:\\s*(\\w+ \\d{1,2}, \\d{4})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return matcher.group(i).trim();
                }
            }
        }
        return "Unknown Date";
    }


    public Mono<Object> queryInvoices(String query, int topK, boolean sortByDate, boolean maxSales) {
        return embeddingUtil.computeEmbedding(query)
                .flatMap(vector -> {
                    Map<String, Object> payload = Map.of(
                            "vector", vector,
                            "topK", topK,
                            "includeMetadata", true
                    );

                    return webClient.post()
                            .uri("/query")
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(Map.class);
                })
                .map(response -> {
                    List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
                        System.out.println("matches::::"+matches);
                    if (matches.isEmpty()) {
                        return List.of(Map.of("error", "No matching invoices found."));
                    }

                    List<Map<String, Object>> invoiceList = new ArrayList<>();

                    for (Map<String, Object> match : matches) {
                        Object metadataObj = match.get("metadata");

                        if (!(metadataObj instanceof Map)) {
                            System.err.println("❌ Warning: Metadata is missing or invalid: " + metadataObj);
                            continue; // Skip this entry
                        }

                        Map<String, Object> metadata = (Map<String, Object>) metadataObj;

                        // Defensive extraction of values
                        String invoiceNumber = metadata.getOrDefault("invoice_number", "Unknown Invoice").toString();
                        String fullText = metadata.getOrDefault("text", "").toString();

                        if (fullText.isEmpty()) {
                            System.err.println("⚠️ Skipping entry: No text found for invoice " + invoiceNumber);
                            continue; // Skip bad entries
                        }

                        System.out.println("✅ Processing Metadata: " + metadata);

                        invoiceList.add(Map.of(
                                "invoice_number", invoiceNumber,
                                "date", extractDate(fullText),  // Ensure extractDate handles empty string
                                "billed_to", extractBilledTo(fullText),  // Ensure extractBilledTo handles empty string
                                "total_amount", extractTotalAmount(fullText),  // Ensure extractTotalAmount handles empty string
                                "full_text", fullText
                        ));
                    }


                    // 🔥 Sorting logic based on user request
                    if (maxSales) {
                        invoiceList.sort((a, b) -> Double.compare(
                                Double.parseDouble((String) b.get("total_amount")),
                                Double.parseDouble((String) a.get("total_amount"))
                        ));
                    } else if (sortByDate) {
                        invoiceList.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date"))); // Latest first
                    }

                    return invoiceList.get(0);
                });
    }


}
