package org.example;


import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
@Component
public class ResourceFileReader  {

    private final ResourcePatternResolver resourcePatternResolver;

    public ResourceFileReader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * Reads all text files from the "documents" folder in src/main/resources.
     *
     * @return List of S3Document containing file name as key and file content.
     */
    public List<S3Document> listDocuments() {
        List<S3Document> documents = new ArrayList<>();
        try {
            // Pattern for files inside src/main/resources/documents
            Resource[] resources = resourcePatternResolver.getResources("classpath:documents/*.txt");
            for (Resource resource : resources) {
                String content = readResourceContent(resource);
                String filename = resource.getFilename();
                documents.add(new S3Document(filename, content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return documents;
    }

    private String readResourceContent(Resource resource) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}

