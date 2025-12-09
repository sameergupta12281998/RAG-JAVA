package com.example.rag.controller;

import com.example.rag.model.Chunk;
import com.example.rag.model.Page;
import com.example.rag.service.*;
import com.example.rag.service.VectorStore.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CrawlerService crawlerService;
    private final ExtractorService extractorService;
    private final ChunkerService chunkerService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final RetrievalService retrievalService;
    private final AnswerService answerService;

    @Value("${crawler.maxDepth:2}")
    private int maxDepth;

    public ApiController(CrawlerService crawlerService, ExtractorService extractorService,
                         ChunkerService chunkerService, EmbeddingService embeddingService,
                         VectorStore vectorStore, RetrievalService retrievalService, AnswerService answerService) {
        this.crawlerService = crawlerService;
        this.extractorService = extractorService;
        this.chunkerService = chunkerService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.retrievalService = retrievalService;
        this.answerService = answerService;
    }

    @PostMapping("/crawl")
    public ResponseEntity<?> crawl(@RequestBody Map<String, String> body) throws IOException {
        String baseUrl = body.get("baseUrl");
        if (baseUrl == null) return ResponseEntity.badRequest().body(Map.of("error", "baseUrl required"));

        // 1. Crawl
        List<Page> pages = crawlerService.crawl(baseUrl);

        // 2. Extract & clean, chunk, embed
        vectorStore.clear();
        int totalChunks = 0;
        for (Page p : pages) {
            String cleaned = extractorService.cleanHtml(p.getHtml());
            List<Chunk> chunks = chunkerService.chunkText(p.getUrl(), p.getTitle(), cleaned, 900, 150);
            for (Chunk c : chunks) {
                try {
                    float[] vec = embeddingService.embedText(c.getText());
                    vectorStore.add(vec, c);
                } catch (Exception e) {
                    // continue on embed failures
                }
            }
            totalChunks += chunks.size();
        }

        // Save vectorstore for persistence
        try { vectorStore.saveToFile("data/vectorstore.dat"); } catch (Exception ignored) {}

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("pagesCrawled", pages.size());
        resp.put("chunksIndexed", totalChunks);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reindex")
    public ResponseEntity<?> reindex() throws IOException {
        // This simple implementation assumes pages are re-crawled and saved externally.
        // For demo: just clears and says success
        vectorStore.clear();
        try { vectorStore.saveToFile("data/vectorstore.dat"); } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("status", "success", "message", "Vectors cleared. Re-run /crawl to reindex."));
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, String> body) throws IOException {
        String question = body.get("question");
        if (question == null) return ResponseEntity.badRequest().body(Map.of("error", "question required"));

        List<SearchResult> retrieved = retrievalService.retrieve(question, 5);
        AnswerService.AnswerResponse ar = answerService.generateAnswer(question, retrieved);

        Map<String, Object> out = new HashMap<>();
        out.put("answer", ar.answer);
        out.put("sources", ar.sources);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/faq/generate")
    public ResponseEntity<?> generateFaq(@RequestBody Map<String, Object> body) throws IOException {
        List<String> questions = (List<String>) body.get("questions");
        if (questions == null) return ResponseEntity.badRequest().body(Map.of("error", "questions required"));

        List<Map<String, Object>> faq = new ArrayList<>();
        for (String q : questions) {
            List<SearchResult> retrieved = retrievalService.retrieve(q, 5);
            AnswerService.AnswerResponse ar = answerService.generateAnswer(q, retrieved);
            faq.add(Map.of("question", q, "answer", ar.answer, "sources", ar.sources));
        }

        // Optionally save to file
        // For simplicity return as response
        return ResponseEntity.ok(Map.of("faq", faq));
    }
}
