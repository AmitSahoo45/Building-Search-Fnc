package com.learning.news_search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.SearchHit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {
    private final NewsRepository repository;
    private final ObjectMapper objectMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    public NewsService(NewsRepository repository, ObjectMapper objectMapper, ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
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

    public Page<NewsArticle> searchArticles(String keyword, Pageable pageable) {
        return repository.searchByKeyword(keyword, pageable);
    }

    public SearchResponse searchWithAggregations(String keyword, Pageable pageable) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(m -> m
                                .fields("headline^3", "shortDescription")
                                .query(keyword)
                        ))
                .withPageable(pageable)
                .withAggregation("categories", co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .terms(t -> t.field("category").size(10))
                ))
                .build();

        SearchHits<NewsArticle> searchHits = elasticsearchOperations.search(query, NewsArticle.class);

        // 1. Extract Articles
        List<NewsArticle> articles = new ArrayList<>();
        for (SearchHit<NewsArticle> hit : searchHits.getSearchHits()) {
            articles.add(hit.getContent());
        }

        // 2. Extract Category Counts (The tricky part)
        Map<String, Long> categoryCounts = new HashMap<>();
        if (searchHits.getAggregations() != null) {
            // Get the wrapper
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) searchHits.getAggregations();
            // Get our specific aggregation
            ElasticsearchAggregation categoriesAgg = aggs.get("categories");

            if (categoriesAgg != null) {
                // Get the raw Elasticsearch Aggregate object
                Aggregate aggregate = categoriesAgg.aggregation().getAggregate();

                // Check if it's the type we expect (String Terms) and loop through buckets
                if (aggregate.isSterms()) {
                    List<StringTermsBucket> buckets = aggregate.sterms().buckets().array();
                    for (StringTermsBucket bucket : buckets) {
                        categoryCounts.put(bucket.key().stringValue(), bucket.docCount());
                    }
                }
            }
        }

        return new SearchResponse(articles, categoryCounts);
    }
}
