package com.learning.news_search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.SearchHit;

/**
 * Enhanced NewsService with:
 * - A/B testing integration
 * - Event logging for analytics
 * - Custom re-ranking with weighted scoring
 */
@Service
public class NewsService {

    private final NewsRepository repository;
    private final ObjectMapper objectMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RankingService rankingService;
    private final ABTestService abTestService;
    private final SearchEventRepository searchEventRepository;
    private final ClickEventRepository clickEventRepository;
    private final RankingConfig config;

    public NewsService(NewsRepository repository,
                       ObjectMapper objectMapper,
                       ElasticsearchOperations elasticsearchOperations,
                       RankingService rankingService,
                       ABTestService abTestService,
                       SearchEventRepository searchEventRepository,
                       ClickEventRepository clickEventRepository,
                       RankingConfig config) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.elasticsearchOperations = elasticsearchOperations;
        this.rankingService = rankingService;
        this.abTestService = abTestService;
        this.searchEventRepository = searchEventRepository;
        this.clickEventRepository = clickEventRepository;
        this.config = config;
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

    /**
     * Main search endpoint with A/B testing and logging.
     */
    public SearchResponse searchWithABTest(String keyword, String categoryFilter,
                                           String sessionId, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        // Assign A/B variant
        String variant = abTestService.assignVariant(sessionId);
        boolean useNewRanking = abTestService.useNewRanking(variant);

        SearchResponse response;
        if (useNewRanking) {
            response = searchWithReranking(keyword, categoryFilter, pageable);
        } else {
            response = searchWithAggregations(keyword, pageable);
        }

        long responseTime = System.currentTimeMillis() - startTime;

        // Log search event
        logSearchEvent(keyword, categoryFilter, sessionId, variant, response, responseTime);

        return response;
    }

    /**
     * Variant B: Custom re-ranking with weighted features.
     */
    private SearchResponse searchWithReranking(String keyword, String categoryFilter, Pageable pageable) {
        // Fetch larger pool for re-ranking
        int poolSize = config.getReRankPoolSize();
        Pageable largePageable = PageRequest.of(0, poolSize);

        // Build query with category filter if provided
        Query query = buildSearchQuery(keyword, categoryFilter, largePageable);

        SearchHits<NewsArticle> searchHits = elasticsearchOperations.search(query, NewsArticle.class);

        // Extract articles and scores
        List<NewsArticle> articles = new ArrayList<>();
        List<Double> esScores = new ArrayList<>();

        for (SearchHit<NewsArticle> hit : searchHits.getSearchHits()) {
            articles.add(hit.getContent());
            esScores.add((double) hit.getScore());
        }

        // Re-rank using our custom scoring
        List<NewsArticle> reRanked = rankingService.reRank(articles, esScores, categoryFilter);

        // Apply pagination to re-ranked results
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), reRanked.size());
        List<NewsArticle> pageResults = (start < reRanked.size())
                ? reRanked.subList(start, end)
                : new ArrayList<>();

        // Extract category counts
        Map<String, Long> categoryCounts = extractCategoryCounts(searchHits);

        return new SearchResponse(pageResults, categoryCounts);
    }

    /**
     * Variant A: Original ES-based ranking (your existing implementation).
     */
    public SearchResponse searchWithAggregations(String keyword, Pageable pageable) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .functionScore(fs -> fs
                                .query(mq -> mq
                                        .multiMatch(m -> m
                                                .fields("headline^3", "shortDescription")
                                                .query(keyword)
                                        )
                                )
                                .functions(f -> f
                                        .fieldValueFactor(fv -> fv
                                                .field("clickCount")
                                                .modifier(FieldValueFactorModifier.Log1p)
                                                .factor(1.0)
                                                .missing(0.0)
                                        )
                                )
                                .boostMode(FunctionBoostMode.Sum)
                        )
                )
                .withPageable(pageable)
                .withAggregation("categories", co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .terms(t -> t.field("category").size(10))
                ))
                .build();

        SearchHits<NewsArticle> searchHits = elasticsearchOperations.search(query, NewsArticle.class);

        List<NewsArticle> articles = new ArrayList<>();
        for (SearchHit<NewsArticle> hit : searchHits.getSearchHits()) {
            articles.add(hit.getContent());
        }

        Map<String, Long> categoryCounts = extractCategoryCounts(searchHits);

        return new SearchResponse(articles, categoryCounts);
    }

    private Query buildSearchQuery(String keyword, String categoryFilter, Pageable pageable) {
        return NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            // Main text search
                            b.must(m -> m
                                    .multiMatch(mm -> mm
                                            .fields("headline^3", "short_description")
                                            .query(keyword)
                                    )
                            );
                            // Optional category filter
                            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                                b.filter(f -> f
                                        .term(t -> t
                                                .field("category")
                                                .value(categoryFilter)
                                        )
                                );
                            }
                            return b;
                        })
                )
                .withPageable(pageable)
                .withAggregation("categories", co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                        .terms(t -> t.field("category").size(10))
                ))
                .build();
    }

    private Map<String, Long> extractCategoryCounts(SearchHits<NewsArticle> searchHits) {
        Map<String, Long> categoryCounts = new HashMap<>();
        if (searchHits.getAggregations() != null) {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) searchHits.getAggregations();
            ElasticsearchAggregation categoriesAgg = aggs.get("categories");

            if (categoriesAgg != null) {
                Aggregate aggregate = categoriesAgg.aggregation().getAggregate();
                if (aggregate.isSterms()) {
                    List<StringTermsBucket> buckets = aggregate.sterms().buckets().array();
                    for (StringTermsBucket bucket : buckets) {
                        categoryCounts.put(bucket.key().stringValue(), bucket.docCount());
                    }
                }
            }
        }
        return categoryCounts;
    }

    private void logSearchEvent(String keyword, String categoryFilter, String sessionId,
                                String variant, SearchResponse response, long responseTime) {
        try {
            List<String> resultIds = response.articles().stream()
                    .map(NewsArticle::getId)
                    .limit(20) // Only log top 20 result IDs
                    .toList();

            SearchEvent event = SearchEvent.builder()
                    .sessionId(sessionId)
                    .query(keyword)
                    .categoryFilter(categoryFilter)
                    .resultIds(resultIds)
                    .resultCount(response.articles().size())
                    .responseTimeMs(responseTime)
                    .rankingVariant(variant)
                    .build();

            searchEventRepository.save(event);
        } catch (Exception e) {
            // Don't let logging failures break search
            System.err.println("Failed to log search event: " + e.getMessage());
        }
    }

    /**
     * Log a click event with timing and position info.
     */
    public void recordClick(String sessionId, String query, String articleId,
                            Integer position, String variant, Long timeToClickMs) {
        try {
            // Increment article click count
            NewsArticle article = repository.findById(articleId).orElse(null);
            if (article != null) {
                article.setClickCount(article.getClickCount() + 1);
                repository.save(article);
            }

            // Log click event for analytics
            ClickEvent event = ClickEvent.builder()
                    .sessionId(sessionId)
                    .query(query)
                    .articleId(articleId)
                    .position(position)
                    .rankingVariant(variant)
                    .timeToClickMs(timeToClickMs)
                    .build();

            clickEventRepository.save(event);
        } catch (Exception e) {
            System.err.println("Failed to record click: " + e.getMessage());
        }
    }

    // Keep original method for backward compatibility
    public void incrementClickCount(String id) {
        try {
            NewsArticle article = repository.findById(id).orElse(null);
            if (article != null) {
                article.setClickCount(article.getClickCount() + 1);
                repository.save(article);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}