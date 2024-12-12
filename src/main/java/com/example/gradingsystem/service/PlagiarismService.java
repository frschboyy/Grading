package com.example.gradingsystem.service;

import java.util.*;
import java.util.stream.*;

public class PlagiarismService {

    // Normalize text by converting it to lowercase and removing extra spaces
    public String normalizeText(String text) {
        return text.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    // Calculate Jaccard similarity (for simpler plagiarism detection)
    public double calculateJaccardSimilarity(String text1, String text2) {
        text1 = normalizeText(text1);
        text2 = normalizeText(text2);

        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (double) intersection.size() / union.size();
    }

    // Calculate TF-IDF similarity using Cosine Similarity
    public double calculateTFIDFSimilarity(String text1, String text2) {
        String[] tokens1 = text1.split("\\s+");
        String[] tokens2 = text2.split("\\s+");

        Map<String, Integer> termFrequency1 = getTermFrequency(tokens1);
        Map<String, Integer> termFrequency2 = getTermFrequency(tokens2);

        Map<String, Double> idf = calculateIDF(Arrays.asList(tokens1, tokens2));

        Map<String, Double> tfidf1 = computeTFIDF(termFrequency1, idf);
        Map<String, Double> tfidf2 = computeTFIDF(termFrequency2, idf);

        return calculateCosineSimilarity(tfidf1, tfidf2);
    }

    // Helper method to calculate term frequency for a given set of tokens
    private Map<String, Integer> getTermFrequency(String[] tokens) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : tokens) {
            termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
        }
        return termFrequency;
    }

    // Calculate Inverse Document Frequency (IDF)
    private Map<String, Double> calculateIDF(List<String[]> tokenLists) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (String[] tokens : tokenLists) {
            Set<String> uniqueTokens = new HashSet<>(Arrays.asList(tokens));
            for (String token : uniqueTokens) {
                documentFrequency.put(token, documentFrequency.getOrDefault(token, 0) + 1);
            }
        }

        Map<String, Double> idf = new HashMap<>();
        int totalDocuments = tokenLists.size();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            idf.put(entry.getKey(), Math.log((double) totalDocuments / (1 + entry.getValue())));
        }
        return idf;
    }

    // Compute TF-IDF for a term frequency map
    private Map<String, Double> computeTFIDF(Map<String, Integer> termFrequency, Map<String, Double> idf) {
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();
            double termIDF = idf.getOrDefault(term, 0.0);
            tfidf.put(term, frequency * termIDF);
        }
        return tfidf;
    }

    // Calculate Cosine Similarity between two TF-IDF vectors
    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (String term : vector1.keySet()) {
            double val1 = vector1.getOrDefault(term, 0.0);
            double val2 = vector2.getOrDefault(term, 0.0);
            dotProduct += val1 * val2;
            magnitude1 += Math.pow(val1, 2);
            magnitude2 += Math.pow(val2, 2);
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        return (magnitude1 == 0 || magnitude2 == 0) ? 0.0 : dotProduct / (magnitude1 * magnitude2);
    }
}