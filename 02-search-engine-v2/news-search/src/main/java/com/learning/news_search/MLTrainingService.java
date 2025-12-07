package com.learning.news_search;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Service to train and evaluate the ML ranking model.
 */
@Service
public class MLTrainingService {

    private final AnalyticsService analyticsService;
    private final SimpleMLRanker mlRanker;
    private final NewsRepository newsRepository;

    public MLTrainingService(AnalyticsService analyticsService,
                             SimpleMLRanker mlRanker,
                             NewsRepository newsRepository) {
        this.analyticsService = analyticsService;
        this.mlRanker = mlRanker;
        this.newsRepository = newsRepository;
    }

    /**
     * Train the model on historical click data.
     */
    public TrainingResult trainModel(int epochs) {
        List<AnalyticsService.TrainingExample> rawExamples = analyticsService.generateTrainingData();

        if (rawExamples.isEmpty()) {
            return new TrainingResult(false, "No training data available. Generate some search/click events first.");
        }

        // Convert to feature vectors
        List<SimpleMLRanker.TrainingPair> trainingPairs = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Get max clicks for normalization
        int maxClicks = 1;
        for (NewsArticle article : newsRepository.findAll()) {
            if (article.getClickCount() != null && article.getClickCount() > maxClicks) {
                maxClicks = article.getClickCount();
            }
        }

        for (AnalyticsService.TrainingExample example : rawExamples) {
            Optional<NewsArticle> articleOpt = newsRepository.findById(example.docId());
            if (articleOpt.isEmpty()) continue;

            NewsArticle article = articleOpt.get();

            // Extract features (using placeholder ES score of 0.5 since we don't have it stored)
            double esScore = 0.5; // Would need to store this in click events for real system
            double popularityScore = Math.log1p(article.getClickCount() != null ? article.getClickCount() : 0)
                    / Math.log1p(maxClicks);

            double freshnessScore = 0.5;
            if (article.getDate() != null) {
                long ageInDays = java.time.temporal.ChronoUnit.DAYS.between(article.getDate(), today);
                freshnessScore = 1.0 / (1.0 + ageInDays / 30.0);
            }

            double categoryBoost = 1.0; // Would need category filter from query

            double[] features = {esScore, popularityScore, freshnessScore, categoryBoost};
            trainingPairs.add(new SimpleMLRanker.TrainingPair(features, example.label()));
        }

        if (trainingPairs.isEmpty()) {
            return new TrainingResult(false, "Could not build training pairs. Check article IDs in events.");
        }

        // Balance dataset (optional but helps with class imbalance)
        List<SimpleMLRanker.TrainingPair> balanced = balanceDataset(trainingPairs);

        // Train
        mlRanker.train(balanced, epochs);

        SimpleMLRanker.ModelWeights weights = mlRanker.getWeights();

        return new TrainingResult(
                true,
                String.format("Trained on %d examples (%d balanced). Weights: es=%.3f, pop=%.3f, fresh=%.3f, cat=%.3f",
                        trainingPairs.size(), balanced.size(),
                        weights.weights()[0], weights.weights()[1],
                        weights.weights()[2], weights.weights()[3])
        );
    }

    /**
     * Balance positive/negative examples by upsampling minority class.
     */
    private List<SimpleMLRanker.TrainingPair> balanceDataset(List<SimpleMLRanker.TrainingPair> data) {
        List<SimpleMLRanker.TrainingPair> positives = new ArrayList<>();
        List<SimpleMLRanker.TrainingPair> negatives = new ArrayList<>();

        for (SimpleMLRanker.TrainingPair pair : data) {
            if (pair.label() == 1) {
                positives.add(pair);
            } else {
                negatives.add(pair);
            }
        }

        // Upsample minority class
        List<SimpleMLRanker.TrainingPair> balanced = new ArrayList<>();
        int targetSize = Math.max(positives.size(), negatives.size());

        Random rand = new Random(42);

        while (balanced.size() < targetSize) {
            if (!positives.isEmpty()) {
                balanced.add(positives.get(rand.nextInt(positives.size())));
            }
        }
        while (balanced.size() < targetSize * 2) {
            if (!negatives.isEmpty()) {
                balanced.add(negatives.get(rand.nextInt(negatives.size())));
            }
        }

        Collections.shuffle(balanced, rand);
        return balanced;
    }

    /**
     * Manually set model weights (for tuning without full training).
     */
    public void setWeights(double esWeight, double popWeight, double freshWeight,
                           double catWeight, double bias) {
        mlRanker.setWeights(new double[]{esWeight, popWeight, freshWeight, catWeight}, bias);
    }

    /**
     * Get current model weights.
     */
    public SimpleMLRanker.ModelWeights getWeights() {
        return mlRanker.getWeights();
    }

    public record TrainingResult(boolean success, String message) {
    }
}