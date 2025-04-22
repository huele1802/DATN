package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.service.EmbeddingSimilarityChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimilarityCheckController {

    private final EmbeddingSimilarityChecker similarityChecker;

    @Autowired
    public SimilarityCheckController(EmbeddingSimilarityChecker similarityChecker) {
        this.similarityChecker = similarityChecker;
    }

    @GetMapping("/check-similarity")
    public String checkSimilarity(@RequestParam String query) {
        similarityChecker.checkSimilarity(query);
        return "Similarity check completed. Please check the logs for results.";
    }
}