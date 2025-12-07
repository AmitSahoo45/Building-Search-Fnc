package com.learning.news_search;

/**
 * Wraps a NewsArticle with computed ranking features.
 * Used for re-ranking after ES retrieval.
 */
public class ScoredArticle implements Comparable<ScoredArticle> {

    private final NewsArticle article;
    private final double esScore;          // Original BM25/function_score from ES
    private final double popularityScore;  // Normalized clicks + views
    private final double freshnessScore;   // 1 / (age_in_days + 1)
    private final double categoryBoost;    // 1.0 or higher if category matches
    private double finalScore;             // Weighted combination

    public ScoredArticle(NewsArticle article, double esScore, double popularityScore,
                         double freshnessScore, double categoryBoost) {
        this.article = article;
        this.esScore = esScore;
        this.popularityScore = popularityScore;
        this.freshnessScore = freshnessScore;
        this.categoryBoost = categoryBoost;
        this.finalScore = 0.0;
    }

    /**
     * Compute final score using weighted sum formula.
     */
    public void computeFinalScore(double w1, double w2, double w3, double categoryMultiplier) {
        this.finalScore = (w1 * esScore + w2 * popularityScore + w3 * freshnessScore) * categoryBoost;
    }

    /**
     * For ML: returns feature vector [esScore, popularityScore, freshnessScore, categoryBoost]
     */
    public double[] getFeatureVector() {
        return new double[] { esScore, popularityScore, freshnessScore, categoryBoost };
    }

    @Override
    public int compareTo(ScoredArticle other) {
        // Descending order (higher score first)
        return Double.compare(other.finalScore, this.finalScore);
    }

    // Getters
    public NewsArticle getArticle() { return article; }
    public double getEsScore() { return esScore; }
    public double getPopularityScore() { return popularityScore; }
    public double getFreshnessScore() { return freshnessScore; }
    public double getCategoryBoost() { return categoryBoost; }
    public double getFinalScore() { return finalScore; }

    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
}