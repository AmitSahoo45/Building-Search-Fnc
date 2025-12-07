package com.learning.news_search;

import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles A/B test variant assignment.
 * Variant A = Old ranking (ES default with basic boosts)
 * Variant B = New ranking (custom weighted scoring + re-ranking)
 */
@Service
public class ABTestService {

    private final RankingConfig config;

    public ABTestService(RankingConfig config) {
        this.config = config;
    }

    /**
     * Determines which variant a request should use.
     * Uses session ID for consistent assignment (same user always gets same variant).
     */
    public String assignVariant(String sessionId) {
        if (sessionId == null || sessionId.isEmpty())
            // No session - random assignment
            return randomAssignment();


        // Use hash of session ID for consistent assignment
        // Same session always gets same variant
        int hash = Math.abs(sessionId.hashCode());
        double bucket = (hash % 100) / 100.0;

        return bucket < config.getAbTestPercentage() ? "B" : "A";
    }

    // Random assignment for anonymous requests.
    private String randomAssignment() {
        double rand = ThreadLocalRandom.current().nextDouble();
        return rand < config.getAbTestPercentage() ? "B" : "A";
    }

    // Check if we should use the new ranking algorithm.
    public boolean useNewRanking(String variant) {
        return "B".equals(variant);
    }
}