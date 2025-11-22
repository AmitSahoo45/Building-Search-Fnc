package com.learning.news_search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;

@Service
public class NewsService {
    private final NewsRepository repository;
    private final ObjectMapper objectMapper;

    public NewsService(NewsRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void loadDataFromJson() {
        long count = repository.count();
        if (count > 0) {
            System.out.println("Data already exists (" + count + " docs). Skipping load.");
            return;
        }

        System.out.println("Starting data load...");
        int loadedCount = 0;

        try {
            // ClassPathResource looks inside src/main/resources
            ClassPathResource resource = new ClassPathResource("json/News_Category_Dataset.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                NewsArticle article = objectMapper.readValue(line, NewsArticle.class);
                repository.save(article);
                loadedCount++;
                if (loadedCount % 500 == 0) System.out.println("Loaded " + loadedCount + "...");
            }
            System.out.println("Finished! Total loaded: " + loadedCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
