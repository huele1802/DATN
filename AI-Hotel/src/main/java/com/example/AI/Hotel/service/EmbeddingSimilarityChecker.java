package com.example.AI.Hotel.service;

import com.example.AI.Hotel.model.HotelEmbedding;
import com.example.AI.Hotel.repository.HotelEmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingSimilarityChecker {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingSimilarityChecker.class);
    private static final String EMBEDDING_API_URL = "http://localhost:8000/embed";

    private final HotelEmbeddingRepository hotelEmbeddingBackupRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public EmbeddingSimilarityChecker(HotelEmbeddingRepository hotelEmbeddingBackupRepository) {
        this.hotelEmbeddingBackupRepository = hotelEmbeddingBackupRepository;
        this.restTemplate = new RestTemplate();
    }

    public void checkSimilarity(String query) {
        logger.info("Checking similarity for query: {}", query);

        try {
            // Bước 1: Lấy embedding của truy vấn từ API
            float[] queryEmbedding = getQueryEmbedding(query);
            logger.debug("Query embedding created, length: {}", queryEmbedding.length);

            // Bước 2: Lấy tất cả embedding từ bảng hotel_embeddings_backup
            List<HotelEmbedding> embeddings = hotelEmbeddingBackupRepository.findAll();
            logger.info("Found {} embeddings in hotel_embeddings_backup", embeddings.size());

            if (embeddings.isEmpty()) {
                logger.warn("No embeddings found in hotel_embeddings_backup");
                return;
            }

            // Bước 3: Tính độ tương đồng cosine
            List<SimilarityResult> results = new ArrayList<>();
            for (HotelEmbedding embedding : embeddings) {
                try {
                    float[] hotelEmbedding = parseEmbedding(embedding.getTextEmbedding());
                    double similarity = calculateCosineSimilarity(queryEmbedding, hotelEmbedding);
                    results.add(new SimilarityResult(embedding.getHotelId(), similarity));
                } catch (Exception e) {
                    logger.error("Error parsing embedding for hotel ID {}: {}", embedding.getHotelId(), e.getMessage());
                }
            }

            // Bước 4: Sắp xếp và in top 10 kết quả
            results.sort(Comparator.comparingDouble(SimilarityResult::getSimilarity).reversed());
            logger.info("Top 10 hotels with highest similarity to query '{}':", query);
            for (int i = 0; i < Math.min(10, results.size()); i++) {
                SimilarityResult result = results.get(i);
                logger.info("Hotel ID: {}, Similarity: {}", result.getHotelId(), result.getSimilarity());
            }

        } catch (Exception e) {
            logger.error("Error checking similarity: {}", e.getMessage(), e);
        }
    }

    private float[] getQueryEmbedding(String query) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("query", query);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    EMBEDDING_API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Double> embeddingList = (List<Double>) response.getBody().get("embedding");
                if (embeddingList.size() != 768) {
                    throw new RuntimeException("Invalid embedding dimension: " + embeddingList.size());
                }
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }
                return embedding;
            } else {
                throw new RuntimeException("Failed to get embedding from API: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error calling embedding API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get query embedding", e);
        }
    }

    private float[] parseEmbedding(String embeddingString) {
        try {
            // Xử lý cả định dạng [x1,x2,...,x768] và x1,x2,...,x768
            String cleaned = embeddingString.replaceAll("[\\[\\]\\s]", "");
            String[] parts = cleaned.split(",");
            float[] embedding = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                embedding[i] = Float.parseFloat(parts[i]);
            }
            return embedding;
        } catch (Exception e) {
            logger.error("Failed to parse embedding: {}", embeddingString, e);
            throw new RuntimeException("Invalid embedding format", e);
        }
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    // Class để lưu kết quả độ tương đồng
    private static class SimilarityResult {
        private final Integer hotelId;
        private final double similarity;

        public SimilarityResult(Integer hotelId, double similarity) {
            this.hotelId = hotelId;
            this.similarity = similarity;
        }

        public Integer getHotelId() {
            return hotelId;
        }

        public double getSimilarity() {
            return similarity;
        }
    }
}