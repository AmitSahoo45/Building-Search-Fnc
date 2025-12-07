package com.learning.news_search;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Analytics service for measuring search quality metrics.
 *
 * Key metrics:
 * - CTR (Click-Through Rate): clicks / impressions
 * - No-click rate: searches with no clicks / total searches
 * - Mean time-to-click: avg time from search to first click
 * - Position bias: where do clicks happen (rank 1, 2, 3...)?
 */
@Service
public class AnalyticsService {

    private final SearchEventRepository searchEventRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(SearchEventRepository searchEventRepository,
                            ClickEventRepository clickEventRepository) {
        this.searchEventRepository = searchEventRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * Calculate metrics for each A/B variant over the last N hours.
     */
    public Map<String, VariantMetrics> getABTestMetrics(int hoursBack) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        Instant now = Instant.now();

        // Get all events in time range
        List<SearchEvent> searches = searchEventRepository.findByTimestampBetween(since, now);
        List<ClickEvent> clicks = clickEventRepository.findByTimestampBetween(since, now);

        // Group by variant
        Map<String, List<SearchEvent>> searchesByVariant = searches.stream()
                .collect(Collectors.groupingBy(SearchEvent::getRankingVariant));

        Map<String, List<ClickEvent>> clicksByVariant = clicks.stream()
                .collect(Collectors.groupingBy(ClickEvent::getRankingVariant));

        Map<String, VariantMetrics> results = new HashMap<>();

        for (String variant : Arrays.asList("A", "B")) {
            List<SearchEvent> variantSearches = searchesByVariant.getOrDefault(variant, Collections.emptyList());
            List<ClickEvent> variantClicks = clicksByVariant.getOrDefault(variant, Collections.emptyList());

            results.put(variant, computeMetrics(variantSearches, variantClicks));
        }

        return results;
    }

    private VariantMetrics computeMetrics(List<SearchEvent> searches, List<ClickEvent> clicks) {
        if (searches.isEmpty()) {
            return new VariantMetrics(0, 0, 0.0, 0.0, 0.0, new HashMap<>());
        }

        int totalSearches = searches.size();
        int totalClicks = clicks.size();

        // CTR = clicks / (searches * avg_results_per_search)
        // Simplified: clicks / searches gives click rate per search
        double ctr = totalSearches > 0 ? (double) totalClicks / totalSearches : 0.0;

        // Group searches by session to find no-click sessions
        Set<String> sessionsWithSearches = searches.stream()
                .map(SearchEvent::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> sessionsWithClicks = clicks.stream()
                .map(ClickEvent::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long noClickSessions = sessionsWithSearches.stream()
                .filter(s -> !sessionsWithClicks.contains(s))
                .count();

        double noClickRate = sessionsWithSearches.isEmpty() ? 0.0 :
                (double) noClickSessions / sessionsWithSearches.size();

        // Mean time-to-click
        double avgTimeToClick = clicks.stream()
                .filter(c -> c.getTimeToClickMs() != null)
                .mapToLong(ClickEvent::getTimeToClickMs)
                .average()
                .orElse(0.0);

        // Position distribution
        Map<Integer, Long> positionCounts = clicks.stream()
                .filter(c -> c.getPosition() != null)
                .collect(Collectors.groupingBy(ClickEvent::getPosition, Collectors.counting()));

        return new VariantMetrics(totalSearches, totalClicks, ctr, noClickRate, avgTimeToClick, positionCounts);
    }

    /**
     * Get top queries with zero clicks (potential relevance issues).
     */
    public List<QueryStats> getProblematicQueries(int hoursBack, int limit) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        Instant now = Instant.now();

        List<SearchEvent> searches = searchEventRepository.findByTimestampBetween(since, now);
        List<ClickEvent> clicks = clickEventRepository.findByTimestampBetween(since, now);

        // Count searches per query
        Map<String, Long> searchCountByQuery = searches.stream()
                .filter(s -> s.getQuery() != null)
                .collect(Collectors.groupingBy(SearchEvent::getQuery, Collectors.counting()));

        // Count clicks per query
        Map<String, Long> clickCountByQuery = clicks.stream()
                .filter(c -> c.getQuery() != null)
                .collect(Collectors.groupingBy(ClickEvent::getQuery, Collectors.counting()));

        // Find queries with low CTR
        List<QueryStats> stats = searchCountByQuery.entrySet().stream()
                .map(e -> {
                    String query = e.getKey();
                    long searchCount = e.getValue();
                    long clickCount = clickCountByQuery.getOrDefault(query, 0L);
                    double ctr = (double) clickCount / searchCount;
                    return new QueryStats(query, searchCount, clickCount, ctr);
                })
                .filter(qs -> qs.searchCount() >= 3) // Only queries with enough data
                .sorted(Comparator.comparingDouble(QueryStats::ctr)) // Lowest CTR first
                .limit(limit)
                .collect(Collectors.toList());

        return stats;
    }

    /**
     * Generate training data for ML model.
     * Returns list of (features, label) for each query-doc pair.
     */
    public List<TrainingExample> generateTrainingData() {
        // Get all clicks grouped by query
        Iterable<ClickEvent> allClicks = clickEventRepository.findAll();
        Iterable<SearchEvent> allSearches = searchEventRepository.findAll();

        Map<String, Set<String>> clickedDocsByQuery = StreamSupport.stream(allClicks.spliterator(), false)
                .filter(c -> c.getQuery() != null && c.getArticleId() != null)
                .collect(Collectors.groupingBy(
                        ClickEvent::getQuery,
                        Collectors.mapping(ClickEvent::getArticleId, Collectors.toSet())
                ));

        // For each search, the shown but not clicked docs are negative examples
        List<TrainingExample> examples = new ArrayList<>();

        for (SearchEvent search : allSearches) {
            if (search.getQuery() == null || search.getResultIds() == null) continue;

            Set<String> clicked = clickedDocsByQuery.getOrDefault(search.getQuery(), Collections.emptySet());

            for (String docId : search.getResultIds()) {
                int label = clicked.contains(docId) ? 1 : 0;
                examples.add(new TrainingExample(search.getQuery(), docId, label));
            }
        }

        return examples;
    }

    // Record classes for clean data transfer
    public record VariantMetrics(
            int totalSearches,
            int totalClicks,
            double ctr,
            double noClickRate,
            double avgTimeToClickMs,
            Map<Integer, Long> clicksByPosition
    ) {}

    public record QueryStats(
            String query,
            long searchCount,
            long clickCount,
            double ctr
    ) {}

    public record TrainingExample(
            String query,
            String docId,
            int label // 1 = clicked, 0 = not clicked
    ) {}
}