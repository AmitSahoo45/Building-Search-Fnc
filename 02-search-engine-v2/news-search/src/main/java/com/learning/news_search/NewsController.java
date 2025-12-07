package com.learning.news_search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService service;
    private final AnalyticsService analyticsService;

    public NewsController(NewsService service, AnalyticsService analyticsService) {
        this.service = service;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Search Engine is Online!";
    }

    @PostMapping("/load")
    public String loadData() {
        service.loadDataFromJson();
        return "Data loading triggered! Check the console for details.";
    }

    /**
     * Original simple search (no A/B testing).
     */
    @GetMapping("/search")
    public Page<NewsArticle> searchArticles(@RequestParam String keyword, Pageable pageable) {
        return service.searchArticles(keyword, pageable);
    }

    /**
     * Original aggregated search (no A/B testing).
     */
    @GetMapping("/searchAgg")
    public SearchResponse searchWithAggregations(@RequestParam String keyword, Pageable pageable) {
        return service.searchWithAggregations(keyword, pageable);
    }

    /**
     * NEW: A/B tested search with logging.
     *
     * Headers:
     * - X-Session-Id: Client session ID (for consistent variant assignment)
     *
     * Response headers include:
     * - X-Ranking-Variant: Which variant was used (A or B)
     */
    @GetMapping("/v2/search")
    public SearchResponse searchWithABTest(
            @RequestParam String keyword,
            @RequestParam(required = false) String category,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            Pageable pageable) {
        return service.searchWithABTest(keyword, category, sessionId, pageable);
    }

    /**
     * Original click counter.
     */
    @PostMapping("/click/{id}")
    public void incrementClickCount(@PathVariable String id) {
        service.incrementClickCount(id);
    }

    /**
     * NEW: Enhanced click tracking with full context.
     *
     * Use this for proper analytics - it logs:
     * - Which query led to the click
     * - Position in results
     * - A/B variant
     * - Time-to-click
     */
    @PostMapping("/v2/click")
    public void recordClick(@RequestBody ClickRequest request) {
        service.recordClick(
                request.sessionId(),
                request.query(),
                request.articleId(),
                request.position(),
                request.variant(),
                request.timeToClickMs()
        );
    }

    // ==================== Analytics Endpoints ====================

    /**
     * Get A/B test metrics comparing variants A and B.
     *
     * @param hours Look back this many hours (default: 24)
     */
    @GetMapping("/analytics/ab-test")
    public Map<String, AnalyticsService.VariantMetrics> getABTestMetrics(
            @RequestParam(defaultValue = "24") int hours) {
        return analyticsService.getABTestMetrics(hours);
    }

    /**
     * Get queries with low CTR (potential relevance issues).
     *
     * @param hours Look back this many hours
     * @param limit Max number of queries to return
     */
    @GetMapping("/analytics/problem-queries")
    public List<AnalyticsService.QueryStats> getProblematicQueries(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "20") int limit) {
        return analyticsService.getProblematicQueries(hours, limit);
    }

    /**
     * Generate training data for ML model.
     * Returns query-doc pairs with click labels.
     */
    @GetMapping("/analytics/training-data")
    public List<AnalyticsService.TrainingExample> getTrainingData() {
        return analyticsService.generateTrainingData();
    }

    /**
     * Get current ranking configuration.
     */
    @GetMapping("/config/ranking")
    public RankingConfigResponse getRankingConfig(RankingConfig config) {
        return new RankingConfigResponse(
                config.getBm25Weight(),
                config.getPopularityWeight(),
                config.getFreshnessWeight(),
                config.getCategoryMatchBoost(),
                config.getAbTestPercentage(),
                config.isMlRerankEnabled()
        );
    }

    // Request/Response DTOs
    public record ClickRequest(
            String sessionId,
            String query,
            String articleId,
            Integer position,
            String variant,
            Long timeToClickMs
    ) {}

    public record RankingConfigResponse(
            double bm25Weight,
            double popularityWeight,
            double freshnessWeight,
            double categoryMatchBoost,
            double abTestPercentage,
            boolean mlRerankEnabled
    ) {}
}