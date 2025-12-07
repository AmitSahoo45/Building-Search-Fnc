package com.learning.news_search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tunable ranking weights for the weighted sum formula:
 * score_final = w1*bm25 + w2*popularity + w3*freshness + w4*categoryBoost
 *
 * These can be adjusted in application.properties without recompiling.
 */


@Configuration
@ConfigurationProperties(prefix = "ranking")
public class RankingConfig {
    // Weight for BM25/text relevance score
    private double bm25Weight = 1.0;

    // Weight for popularity (clicks + views)
    private double popularityWeight = 0.3;

    // Weight for freshness (newer = higher)
    private double freshnessWeight = 0.2;

    // Boost when category matches user filter
    private double categoryMatchBoost = 1.5;

    // Percentage of traffic for variant B (new ranking)
    private double abTestPercentage = 0.10; // 10%

    // Decay rate for freshness (higher = faster decay)
    private double freshnessDecayDays = 30.0;

    // Max documents to fetch from ES before re-ranking
    private int reRankPoolSize = 100;

    // Enable/disable ML re-ranking
    private boolean mlRerankEnabled = false;

    // Getters and Setters
    public double getBm25Weight() { return bm25Weight; }
    public void setBm25Weight(double bm25Weight) { this.bm25Weight = bm25Weight; }

    public double getPopularityWeight() { return popularityWeight; }
    public void setPopularityWeight(double popularityWeight) { this.popularityWeight = popularityWeight; }

    public double getFreshnessWeight() { return freshnessWeight; }
    public void setFreshnessWeight(double freshnessWeight) { this.freshnessWeight = freshnessWeight; }

    public double getCategoryMatchBoost() { return categoryMatchBoost; }
    public void setCategoryMatchBoost(double categoryMatchBoost) { this.categoryMatchBoost = categoryMatchBoost; }

    public double getAbTestPercentage() { return abTestPercentage; }
    public void setAbTestPercentage(double abTestPercentage) { this.abTestPercentage = abTestPercentage; }

    public double getFreshnessDecayDays() { return freshnessDecayDays; }
    public void setFreshnessDecayDays(double freshnessDecayDays) { this.freshnessDecayDays = freshnessDecayDays; }

    public int getReRankPoolSize() { return reRankPoolSize; }
    public void setReRankPoolSize(int reRankPoolSize) { this.reRankPoolSize = reRankPoolSize; }

    public boolean isMlRerankEnabled() { return mlRerankEnabled; }
    public void setMlRerankEnabled(boolean mlRerankEnabled) { this.mlRerankEnabled = mlRerankEnabled; }
}
