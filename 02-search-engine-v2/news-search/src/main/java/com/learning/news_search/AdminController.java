package com.learning.news_search;

import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoints for model training and configuration.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MLTrainingService mlTrainingService;
    private final RankingConfig rankingConfig;

    public AdminController(MLTrainingService mlTrainingService, RankingConfig rankingConfig) {
        this.mlTrainingService = mlTrainingService;
        this.rankingConfig = rankingConfig;
    }

    /**
     * Train the ML model on historical click data.
     */
    @PostMapping("/train")
    public MLTrainingService.TrainingResult trainModel(
            @RequestParam(defaultValue = "100") int epochs) {
        return mlTrainingService.trainModel(epochs);
    }

    /**
     * Get current ML model weights.
     */
    @GetMapping("/model/weights")
    public SimpleMLRanker.ModelWeights getModelWeights() {
        return mlTrainingService.getWeights();
    }

    /**
     * Manually set model weights.
     */
    @PostMapping("/model/weights")
    public String setModelWeights(@RequestBody WeightsRequest request) {
        mlTrainingService.setWeights(
                request.esWeight(),
                request.popularityWeight(),
                request.freshnessWeight(),
                request.categoryWeight(),
                request.bias()
        );
        return "Weights updated successfully";
    }

    /**
     * Update ranking configuration at runtime.
     */
    @PostMapping("/config/ranking")
    public String updateRankingConfig(@RequestBody RankingConfigRequest request) {
        if (request.bm25Weight() != null) {
            rankingConfig.setBm25Weight(request.bm25Weight());
        }
        if (request.popularityWeight() != null) {
            rankingConfig.setPopularityWeight(request.popularityWeight());
        }
        if (request.freshnessWeight() != null) {
            rankingConfig.setFreshnessWeight(request.freshnessWeight());
        }
        if (request.categoryMatchBoost() != null) {
            rankingConfig.setCategoryMatchBoost(request.categoryMatchBoost());
        }
        if (request.abTestPercentage() != null) {
            rankingConfig.setAbTestPercentage(request.abTestPercentage());
        }
        if (request.mlRerankEnabled() != null) {
            rankingConfig.setMlRerankEnabled(request.mlRerankEnabled());
        }
        return "Config updated";
    }

    /**
     * Get current ranking config.
     */
    @GetMapping("/config/ranking")
    public RankingConfigResponse getRankingConfig() {
        return new RankingConfigResponse(
                rankingConfig.getBm25Weight(),
                rankingConfig.getPopularityWeight(),
                rankingConfig.getFreshnessWeight(),
                rankingConfig.getCategoryMatchBoost(),
                rankingConfig.getAbTestPercentage(),
                rankingConfig.getFreshnessDecayDays(),
                rankingConfig.getReRankPoolSize(),
                rankingConfig.isMlRerankEnabled()
        );
    }

    // Request/Response DTOs
    public record WeightsRequest(
            double esWeight,
            double popularityWeight,
            double freshnessWeight,
            double categoryWeight,
            double bias
    ) {}

    public record RankingConfigRequest(
            Double bm25Weight,
            Double popularityWeight,
            Double freshnessWeight,
            Double categoryMatchBoost,
            Double abTestPercentage,
            Boolean mlRerankEnabled
    ) {}

    public record RankingConfigResponse(
            double bm25Weight,
            double popularityWeight,
            double freshnessWeight,
            double categoryMatchBoost,
            double abTestPercentage,
            double freshnessDecayDays,
            int reRankPoolSize,
            boolean mlRerankEnabled
    ) {}
}