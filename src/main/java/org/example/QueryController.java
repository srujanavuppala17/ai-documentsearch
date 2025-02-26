package org.example;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final PineconeService pineconeService;

    public QueryController(PineconeService pineconeService) {
        this.pineconeService = pineconeService;
    }

    /**
     * API to search invoices by invoice number
     */
    @PostMapping("/query/invoices")
    public Mono<Object> queryInvoices(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "list invoices");
        boolean sortByDate = query.toLowerCase().contains("date") || query.toLowerCase().contains("latest");
        boolean maxSales = query.toLowerCase().contains("highest") || query.toLowerCase().contains("max sales");

        return pineconeService.queryInvoices(query, 5, sortByDate, maxSales);
    }





    /**
     * Extract the invoice date using regex
     */
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

    /**
     * Extract the billed-to company using regex
     */
    private String extractBilledTo(String text) {
        Pattern pattern = Pattern.compile("Billed To:\\s*(.*?)(\\r?\\n|$)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Unknown Company";
    }

    /**
     * Extract the total amount using regex
     */
    private String extractTotalAmount(String text) {
        Pattern pattern = Pattern.compile("Total:\\s*\\$([\\d,.]+)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Unknown Amount";
    }
}
