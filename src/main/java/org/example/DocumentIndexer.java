package org.example;



import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

@Component
public class DocumentIndexer implements CommandLineRunner {


        private final PineconeService pineconeService;
        private final ResourceFileReader resourceFileService;

        public DocumentIndexer(PineconeService pineconeService, ResourceFileReader resourceFileService) {
            this.pineconeService = pineconeService;
            this.resourceFileService = resourceFileService;
        }

    @Override
    public void run(String... args) throws Exception {
        // Retrieve documents from the resources folder (src/main/resources/documents)
        List<S3Document> documents = resourceFileService.listDocuments();

        for (S3Document doc : documents) {
            // Upsert document into Pinecone using the file name as the unique ID
            pineconeService.upsertVector(doc.getKey(), doc.getContent())
                    .doOnSuccess(unused -> System.out.println("Indexed document: " + doc.getKey()))
                    .doOnError(error -> System.err.println("Failed to index document: " + doc.getKey() + " - " + error.getMessage()))
                    .subscribe();  // 🔥 This ensures the request executes
        }
    }

}



