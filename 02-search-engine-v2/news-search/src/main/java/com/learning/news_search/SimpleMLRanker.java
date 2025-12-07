package com.learning.news_search;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;

/**
 * Simple Logistic Regression for Learning-to-Rank.
 * <p>
 * Features: [esScore, popularityScore, freshnessScore, categoryBoost]
 * Label: 1 (clicked) or 0 (not clicked)
 * <p>
 * This is a basic implementation for learning purposes.
 * In production, you'd use XGBoost, LightGBM, or a neural network.
 */
@Component
public class SimpleMLRanker {

    // Model weights (initialized with reasonable defaults)
    private double[] weights = {1.0, 0.3, 0.2, 0.5};
    private double bias = 0.0;
    private double learningRate = 0.01;

    private static final String MODEL_FILE = "ltr_model.bin";

    public SimpleMLRanker() {
        loadModel();
    }

    /**
     * Predict click probability for a feature vector.
     * Uses sigmoid function: P(click) = 1 / (1 + exp(-z))
     * where z = w1*f1 + w2*f2 + ... + bias
     */
    public double predict(double[] features) {
        if (features.length != weights.length) {
            throw new IllegalArgumentException(
                    "Expected " + weights.length + " features, got " + features.length);
        }

        double z = bias;
        for (int i = 0; i < features.length; i++) {
            z += weights[i] * features[i];
        }

        // Sigmoid
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * Train the model on labeled examples.
     * Uses gradient descent on log loss.
     *
     * @param examples List of (features, label) pairs
     * @param epochs   Number of training passes
     */
    public void train(List<TrainingPair> examples, int epochs) {
        System.out.println("Training LTR model on " + examples.size() + " examples...");

        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0.0;

            for (TrainingPair example : examples) {
                double[] features = example.features();
                int label = example.label();

                // Forward pass
                double prediction = predict(features);

                // Compute log loss
                double loss = -label * Math.log(prediction + 1e-10)
                        - (1 - label) * Math.log(1 - prediction + 1e-10);
                totalLoss += loss;

                // Backward pass (gradient descent)
                double error = prediction - label;

                for (int i = 0; i < weights.length; i++) {
                    weights[i] -= learningRate * error * features[i];
                }
                bias -= learningRate * error;
            }

            if (epoch % 10 == 0) {
                double avgLoss = totalLoss / examples.size();
                System.out.printf("Epoch %d: avg loss = %.4f%n", epoch, avgLoss);
            }
        }

        System.out.println("Training complete. Final weights: ");
        System.out.printf("  esScore: %.4f%n", weights[0]);
        System.out.printf("  popularity: %.4f%n", weights[1]);
        System.out.printf("  freshness: %.4f%n", weights[2]);
        System.out.printf("  categoryBoost: %.4f%n", weights[3]);
        System.out.printf("  bias: %.4f%n", bias);

        saveModel();
    }

    /**
     * Re-rank articles using the trained model.
     */
    public void reRank(List<ScoredArticle> articles) {
        for (ScoredArticle article : articles) {
            double probability = predict(article.getFeatureVector());
            article.setFinalScore(probability);
        }

        // Sort by predicted click probability
        articles.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
    }

    /**
     * Save model to file.
     */
    public void saveModel() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(MODEL_FILE))) {
            out.writeInt(weights.length);
            for (double w : weights) {
                out.writeDouble(w);
            }
            out.writeDouble(bias);
            System.out.println("Model saved to " + MODEL_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save model: " + e.getMessage());
        }
    }

    /**
     * Load model from file.
     */
    public void loadModel() {
        File file = new File(MODEL_FILE);
        if (!file.exists()) {
            System.out.println("No saved model found, using defaults.");
            return;
        }

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int numWeights = in.readInt();
            weights = new double[numWeights];
            for (int i = 0; i < numWeights; i++) {
                weights[i] = in.readDouble();
            }
            bias = in.readDouble();
            System.out.println("Model loaded from " + MODEL_FILE);
        } catch (IOException e) {
            System.err.println("Failed to load model: " + e.getMessage());
        }
    }

    /**
     * Get current weights (for debugging/display).
     */
    public ModelWeights getWeights() {
        return new ModelWeights(weights.clone(), bias);
    }

    /**
     * Manually set weights (for tuning).
     */
    public void setWeights(double[] newWeights, double newBias) {
        if (newWeights.length != 4) {
            throw new IllegalArgumentException("Expected 4 weights");
        }
        this.weights = newWeights.clone();
        this.bias = newBias;
        saveModel();
    }

    // Record classes
    public record TrainingPair(double[] features, int label) {
    }

    public record ModelWeights(double[] weights, double bias) {
    }
}