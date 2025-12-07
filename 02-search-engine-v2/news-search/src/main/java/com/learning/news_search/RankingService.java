package com.learning.news_search;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core ranking service implementing:
 * 1. Feature extraction (popularity, freshness, category match)
 * 2. Weighted sum scoring: score = w1*bm25 + w2*popularity + w3*freshness
 * 3. Re-ranking of ES results
 */
@Service
public class RankingService {

    private final RankingConfig config;

    public RankingService(RankingConfig config) {
        this.config = config;
    }

    /**
     * Re-rank articles using our custom weighted scoring.
     *
     * @param articles    Raw articles from ES
     * @param esScores    Corresponding ES scores
     * @param categoryFilter  Optional category filter from user query
     * @return Re-ranked list of articles
     */
    public List<NewsArticle> reRank(List<NewsArticle> articles, List<Double> esScores,
                                    String categoryFilter) {
        if (articles.isEmpty()) {
            return articles;
        }

        // Normalize ES scores to 0-1 range
        double maxEsScore = esScores.stream().mapToDouble(d -> d).max().orElse(1.0);
        double minEsScore = esScores.stream().mapToDouble(d -> d).min().orElse(0.0);
        double esRange = maxEsScore - minEsScore;
        if (esRange == 0) esRange = 1.0;

        // Find max popularity for normalization
        int maxClicks = articles.stream()
                .mapToInt(a -> a.getClickCount() != null ? a.getClickCount() : 0)
                .max().orElse(1);
        if (maxClicks == 0) maxClicks = 1;

        LocalDate today = LocalDate.now();

        // Build scored articles
        List<ScoredArticle> scoredArticles = new ArrayList<>();
        for (int i = 0; i < articles.size(); i++) {
            NewsArticle article = articles.get(i);

            // Normalize ES score to 0-1
            double normalizedEsScore = (esScores.get(i) - minEsScore) / esRange;

            // Popularity: normalized click count (0-1)
            double popularityScore = computePopularityScore(article, maxClicks);

            // Freshness: decays with age
            double freshnessScore = computeFreshnessScore(article, today);

            // Category boost: if user specified category and it matches
            double categoryBoost = computeCategoryBoost(article, categoryFilter);

            ScoredArticle scored = new ScoredArticle(
                    article, normalizedEsScore, popularityScore, freshnessScore, categoryBoost
            );

            // Apply weighted sum
            scored.computeFinalScore(
                    config.getBm25Weight(),
                    config.getPopularityWeight(),
                    config.getFreshnessWeight(),
                    config.getCategoryMatchBoost()
            );

            scoredArticles.add(scored);
        }

        // Sort by final score (descending)
        Collections.sort(scoredArticles);

        // Extract articles in new order
        List<NewsArticle> reRanked = new ArrayList<>();
        for (ScoredArticle sa : scoredArticles) {
            reRanked.add(sa.getArticle());
        }

        return reRanked;
    }

    /**
     * Compute popularity score (normalized to 0-1).
     * Uses log scale to prevent viral articles from dominating.
     */
    private double computePopularityScore(NewsArticle article, int maxClicks) {
        int clicks = article.getClickCount() != null ? article.getClickCount() : 0;

        // Log scale: log(1 + clicks) / log(1 + maxClicks)
        return Math.log1p(clicks) / Math.log1p(maxClicks);
    }

    /**
     * Compute freshness score.
     * Score = 1 / (1 + age_in_days / decay_rate)
     *
     * This gives:
     * - Today's article: ~1.0
     * - 30-day old article: ~0.5 (with default decay of 30)
     * - 90-day old article: ~0.25
     */
    private double computeFreshnessScore(NewsArticle article, LocalDate today) {
        if (article.getDate() == null) {
            return 0.1; // Default for articles without dates
        }

        long ageInDays = ChronoUnit.DAYS.between(article.getDate(), today);
        if (ageInDays < 0) ageInDays = 0; // Future dates treated as today

        return 1.0 / (1.0 + ageInDays / config.getFreshnessDecayDays());
    }

    /**
     * Compute category match boost.
     * Returns boost multiplier if category matches filter, else 1.0.
     */
    private double computeCategoryBoost(NewsArticle article, String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isEmpty()) {
            return 1.0; // No filter, no boost
        }

        if (categoryFilter.equalsIgnoreCase(article.getCategory())) {
            return config.getCategoryMatchBoost();
        }

        return 1.0;
    }

    /**
     * Get feature vector for an article (for ML training/prediction).
     */
    public double[] extractFeatures(NewsArticle article, double esScore,
                                    String categoryFilter, LocalDate today, int maxClicks) {
        double normalizedEsScore = esScore; // Assume already normalized
        double popularityScore = computePopularityScore(article, maxClicks);
        double freshnessScore = computeFreshnessScore(article, today);
        double categoryBoost = computeCategoryBoost(article, categoryFilter);

        return new double[] { normalizedEsScore, popularityScore, freshnessScore, categoryBoost };
    }
}